package com.example.demo.admin.dashboard;

import java.util.List;

public record AdminDashboardSummaryDto(
        long lessonsCreated,
        long usersSignedUp,
        long lessonsEnrolled,
        long newContributors,
        List<AdminDashboardTopConceptDto> topConcepts
) {
}
