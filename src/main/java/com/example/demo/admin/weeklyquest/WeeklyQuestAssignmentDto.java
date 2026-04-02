package com.example.demo.admin.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.weeklyquest.WeeklyQuestAssignment;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;

public record WeeklyQuestAssignmentDto(
        UUID publicId,
        short slotIndex,
        boolean official,
        WeeklyQuestAssignmentSourceType sourceType,
        WeeklyQuestAssignmentStatus status,
        UUID createdByAdminId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        WeeklyQuestConceptDto concept
) {
    public static WeeklyQuestAssignmentDto from(WeeklyQuestAssignment assignment) {
        return new WeeklyQuestAssignmentDto(
                assignment.getPublicId(),
                assignment.getSlotIndex(),
                assignment.isOfficial(),
                assignment.getSourceType(),
                assignment.getStatus(),
                assignment.getCreatedByAdminId(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt(),
                WeeklyQuestConceptDto.from(assignment.getConcept())
        );
    }
}
