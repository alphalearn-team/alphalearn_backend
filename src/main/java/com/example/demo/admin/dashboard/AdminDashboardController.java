package com.example.demo.admin.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.admin.dashboard.dto.AdminDashboardSummaryDto;

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
    public AdminDashboardSummaryDto getSummary(
            @Parameter(description = "Preset analytics window. Supported values: 7d, 30d, 90d")
            @RequestParam(required = false) String range,
            @Parameter(description = "Custom range start date in ISO format (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Custom range end date in ISO format (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return adminDashboardService.getSummary(range, startDate, endDate);
    }
}
