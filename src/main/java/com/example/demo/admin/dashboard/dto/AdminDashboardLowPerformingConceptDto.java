package com.example.demo.admin.dashboard.dto;

import java.util.UUID;

public record AdminDashboardLowPerformingConceptDto(
        UUID conceptPublicId,
        String title,
        long lessonCount
) {
}
