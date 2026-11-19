package com.taskflow.common.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestObservabilityFilter extends OncePerRequestFilter {
    private static final Logger log =
            LoggerFactory.getLogger(RequestObservabilityFilter.class);

    private static final int MAX_LOG_CHARS = 4000;
    private static final int MAX_CACHE_BYTES = 16 * 1024;

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "confirmpassword",
            "accesstoken",
            "refreshtoken",
            "token",
            "authorization",
            "secret",
            "clientsecret",
            "apikey",
            "cookie",
            "setcookie"
    );

    private final ObjectMapper objectMapper;
    private final boolean bodyLoggingEnabled;

    public RequestObservabilityFilter(
            ObjectMapper objectMapper,
            @Value("${app.observability.log-bodies:false}")
            boolean bodyLoggingEnabled) {

        this.objectMapper = objectMapper;
        this.bodyLoggingEnabled = bodyLoggingEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();
        long startNanos = System.nanoTime();

        String method = request.getMethod();
        String path = request.getRequestURI();

        boolean logBodies =
                bodyLoggingEnabled
                        && path.startsWith("/api/v1/")
                        && !path.startsWith("/api/v1/auth/")
                        && isJson(request.getContentType())
                        && !isMultipart(request.getContentType());

        // The request wrapper caches bytes consumed downstream.
        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(
                        request,
                        MAX_CACHE_BYTES
                );

        // The response wrapper captures bytes written downstream.
        ContentCachingResponseWrapper wrappedResponse =
                new ContentCachingResponseWrapper(response);

        wrappedResponse.setHeader("X-Request-ID", requestId);

        try (MDC.MDCCloseable ignored =
                     MDC.putCloseable("requestId", requestId)) {

            log.info(
                    "HTTP_REQUEST method={} path={} requestId={}",
                    method, path, requestId
            );

            try {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } finally {

                long durationMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startNanos
                );

                int status = wrappedResponse.getStatus();

                if (logBodies) {
                    String requestBody = safeJson(
                            wrappedRequest.getContentAsByteArray()
                    );

                    if (!requestBody.isBlank()) {
                        log.info(
                                "HTTP_REQUEST_BODY method={} path={} body={}",
                                method, path, requestBody
                        );
                    }
                }

                boolean logResponseBody =
                        bodyLoggingEnabled
                                && path.startsWith("/api/v1/")
                                && !path.startsWith("/api/v1/auth/")
                                && isJson(wrappedResponse.getContentType());

                if (logResponseBody) {
                    byte[] bytes =
                            wrappedResponse.getContentAsByteArray();

                    if (bytes.length <= MAX_CACHE_BYTES) {
                        String responseBody = safeJson(bytes);

                        if (!responseBody.isBlank()) {
                            log.info(
                                    "HTTP_RESPONSE_BODY method={} path={} body={}",
                                    method, path, responseBody
                            );
                        }
                    } else {
                        log.info(
                                "HTTP_RESPONSE_BODY method={} path={} body=[omitted: too large]",
                                method, path
                        );
                    }
                }

                if (status >= 500) {
                    log.error(
                            "HTTP_RESPONSE method={} path={} status={} durationMs={} requestId={}",
                            method, path, status, durationMs, requestId
                    );
                } else if (status >= 400) {
                    log.warn(
                            "HTTP_RESPONSE method={} path={} status={} durationMs={} requestId={}",
                            method, path, status, durationMs, requestId
                    );
                } else {
                    log.info(
                            "HTTP_RESPONSE method={} path={} status={} durationMs={} requestId={}",
                            method, path, status, durationMs, requestId
                    );
                }

                if (durationMs > 1000) {
                    log.warn(
                            "SLOW_REQUEST method={} path={} durationMs={} requestId={}",
                            method, path, durationMs, requestId
                    );
                }

                // Critical: send cached bytes to the real client.
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }

        String normalized = contentType.toLowerCase(Locale.ROOT);

        return normalized.contains("application/json")
                || normalized.contains("+json");
    }

    private boolean isMultipart(String contentType) {
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT)
                .startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private String safeJson(byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            return "";
        }

        if (bytes.length > MAX_CACHE_BYTES) {
            return "[omitted: too large]";
        }

        try {
            JsonNode root = objectMapper.readTree(bytes);

            redact(root);

            String json = objectMapper.writeValueAsString(root);

            return json.length() > MAX_LOG_CHARS
                    ? json.substring(0, MAX_LOG_CHARS) + "...[truncated]"
                    : json;

        } catch (Exception ex) {
            // Never fall back to logging raw, unparsed payloads.
            return "[omitted: invalid or unsupported JSON]";
        }
    }

    private void redact(JsonNode node) {

        if (node instanceof ObjectNode objectNode) {

            objectNode.properties().forEach(entry -> {

                String normalizedField =
                        entry.getKey()
                                .replace("_", "")
                                .replace("-", "")
                                .toLowerCase(Locale.ROOT);

                if (SENSITIVE_FIELDS.contains(normalizedField)) {
                    objectNode.put(entry.getKey(), "[REDACTED]");
                } else {
                    redact(entry.getValue());
                }
            });

        } else if (node instanceof ArrayNode arrayNode) {

            arrayNode.forEach(this::redact);
        }
    }
}
