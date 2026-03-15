package com.example.demo.admin.dashboard.dto;

import com.example.demo.admin.dashboard.enums.AdminDashboardAlertLevel;

public record AdminDashboardAlertDto(
        String code,
        AdminDashboardAlertLevel level,
        String message
) {
}
