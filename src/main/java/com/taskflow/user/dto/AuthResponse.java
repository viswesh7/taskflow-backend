package com.taskflow.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        UserResponse user
) {
    public record UserResponse(UUID id, String email, String displayName) {}
}
