package com.example.demo.admin.dashboard;

import java.util.UUID;

public record AdminDashboardTopConceptDto(
        UUID conceptPublicId,
        String title,
        long lessonCount
) {
}
