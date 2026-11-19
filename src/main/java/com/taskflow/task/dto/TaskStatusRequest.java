package com.taskflow.task.dto;
import com.taskflow.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
public record TaskStatusRequest(@NotNull TaskStatus status) {}
