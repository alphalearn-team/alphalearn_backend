package com.example.demo.admin.dashboard.dto;

public record AdminDashboardTrendDto(
        String label,
        long lessonsCreated,
        long usersSignedUp,
        long lessonsEnrolled,
        long newContributors
) {
}
