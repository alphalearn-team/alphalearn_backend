package com.example.demo.admin.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.weeklyquest.WeeklyQuestWeek;
import com.example.demo.weeklyquest.enums.WeeklyQuestActivationSource;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;

public record WeeklyQuestWeekDto(
        UUID publicId,
        OffsetDateTime weekStartAt,
        OffsetDateTime setupDeadlineAt,
        WeeklyQuestWeekStatus status,
        WeeklyQuestActivationSource activationSource,
        OffsetDateTime activatedAt,
        OffsetDateTime createdAt,
        boolean editable,
        WeeklyQuestAssignmentDto officialAssignment,
        boolean unset,
        long daysUntilDeadline,
        boolean shouldShowReminder
) {
    public static WeeklyQuestWeekDto from(
            WeeklyQuestWeek week,
            boolean editable,
            WeeklyQuestAssignmentDto officialAssignment,
            boolean unset,
            long daysUntilDeadline,
            boolean shouldShowReminder
    ) {
        return new WeeklyQuestWeekDto(
                week.getPublicId(),
                week.getWeekStartAt(),
                week.getSetupDeadlineAt(),
                week.getStatus(),
                week.getActivationSource(),
                week.getActivatedAt(),
                week.getCreatedAt(),
                editable,
                officialAssignment,
                unset,
                daysUntilDeadline,
                shouldShowReminder
        );
    }
}
