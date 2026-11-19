package com.taskflow.task.dto;

import com.taskflow.task.entity.TaskPriority;
import com.taskflow.task.entity.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id, String title, String description, TaskStatus status, TaskPriority priority,
        Instant dueDate, Instant completedAt, boolean overdue, UUID categoryId,
        String categoryName, Instant createdAt, Instant updatedAt, Long version
) {}
