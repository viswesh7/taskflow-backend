package com.taskflow.dashboard.controller;

import com.taskflow.dashboard.dto.DashboardSummary;
import com.taskflow.dashboard.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/summary")
    public DashboardSummary summary(Authentication auth) {
        return service.summary(UUID.fromString(auth.getName()));
    }
}
