package com.example.demo.admin.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Admin-only dashboard summary endpoints")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get admin dashboard summary",
            description = "Returns lesson, learner, enrollment, contributor, and top-concept metrics used by the admin dashboard"
    )
    public AdminDashboardSummaryDto getSummary() {
        return adminDashboardService.getSummary();
    }
}
