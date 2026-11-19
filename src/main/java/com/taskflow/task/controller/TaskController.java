package com.taskflow.task.controller;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.task.dto.*;
import com.taskflow.task.entity.*;
import com.taskflow.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(Authentication auth, @Valid @RequestBody TaskCreateRequest request) {
        return service.create(userId(auth), request);
    }

    @GetMapping
    public PageResponse<TaskResponse> search(Authentication auth,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        return service.search(userId(auth), status, priority, categoryId, search, dueBefore,
                page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    public TaskResponse get(Authentication auth, @PathVariable UUID id) {
        return service.get(userId(auth), id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(Authentication auth, @PathVariable UUID id,
                               @Valid @RequestBody TaskUpdateRequest request) {
        return service.update(userId(auth), id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(Authentication auth, @PathVariable UUID id,
                                     @Valid @RequestBody TaskStatusRequest request) {
        return service.changeStatus(userId(auth), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable UUID id) {
        service.delete(userId(auth), id);
    }

    private UUID userId(Authentication auth) { return UUID.fromString(auth.getName()); }
}
