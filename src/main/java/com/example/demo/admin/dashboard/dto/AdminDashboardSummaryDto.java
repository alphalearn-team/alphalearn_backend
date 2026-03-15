package com.example.demo.admin.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminDashboardSummaryDto(
        long lessonsCreated,
        long usersSignedUp,
        long lessonsEnrolled,
        long newContributors,
        List<AdminDashboardTopConceptDto> topConcepts,
        AdminDashboardDeltaDto deltas,
        List<AdminDashboardTrendDto> trends,
        List<AdminDashboardAlertDto> alerts,
        Long pendingModerationCount,
        List<AdminDashboardLowPerformingConceptDto> lowPerformingConcepts
) {

    public AdminDashboardSummaryDto(
            long lessonsCreated,
            long usersSignedUp,
            long lessonsEnrolled,
            long newContributors,
            List<AdminDashboardTopConceptDto> topConcepts
    ) {
        this(
                lessonsCreated,
                usersSignedUp,
                lessonsEnrolled,
                newContributors,
                topConcepts,
                null,
                List.of(),
                List.of(),
                null,
                List.of()
        );
    }

    public AdminDashboardSummaryDto {
        topConcepts = topConcepts == null ? List.of() : List.copyOf(topConcepts);
        trends = trends == null ? List.of() : List.copyOf(trends);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        lowPerformingConcepts = lowPerformingConcepts == null ? List.of() : List.copyOf(lowPerformingConcepts);
    }
}
