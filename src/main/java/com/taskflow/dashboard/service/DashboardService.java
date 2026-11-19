package com.taskflow.dashboard.service;

import com.taskflow.dashboard.dto.DashboardSummary;
import com.taskflow.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class DashboardService {
    private final TaskRepository tasks;
    public DashboardService(TaskRepository tasks) { this.tasks = tasks; }

    public DashboardSummary summary(UUID userId) {
        return new DashboardSummary(tasks.countActive(userId), tasks.countPending(userId),
                tasks.countCompleted(userId), tasks.countOverdue(userId, Instant.now()));
    }
}
