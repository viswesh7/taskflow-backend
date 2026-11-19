package com.taskflow.task.dto;

import com.taskflow.task.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record TaskUpdateRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @NotNull TaskPriority priority,
        Instant dueDate,
        UUID categoryId,
        @NotNull Long version
) {}
