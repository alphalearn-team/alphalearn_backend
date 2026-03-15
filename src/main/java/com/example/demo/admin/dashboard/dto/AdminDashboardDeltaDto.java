package com.example.demo.admin.dashboard.dto;

public record AdminDashboardDeltaDto(
        long lessonsCreated,
        long usersSignedUp,
        long lessonsEnrolled,
        long newContributors
) {
}
