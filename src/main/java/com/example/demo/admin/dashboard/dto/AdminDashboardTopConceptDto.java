package com.example.demo.admin.dashboard.dto;

import java.util.UUID;

public record AdminDashboardTopConceptDto(
        UUID conceptPublicId,
        String title,
        long lessonCount
) {
}
