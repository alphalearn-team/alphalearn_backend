package com.example.demo.me.weeklyquest;

import com.example.demo.weeklyquest.QuestHistoryProjection;
import com.example.demo.weeklyquest.enums.SubmissionVisibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record QuestHistoryItemDto(
        UUID submissionPublicId,
        UUID learnerPublicId,
        String learnerUsername,
        UUID assignmentPublicId,
        UUID weekPublicId,
        OffsetDateTime weekStartAt,
        UUID conceptPublicId,
        String conceptTitle,
        String mediaPublicUrl,
        String mediaContentType,
        String originalFilename,
        String caption,
        OffsetDateTime submittedAt,
        SubmissionVisibility visibility
) {

    public static QuestHistoryItemDto from(QuestHistoryProjection projection) {
        return new QuestHistoryItemDto(
                projection.getSubmissionPublicId(),
                projection.getLearnerPublicId(),
                projection.getLearnerUsername(),
                projection.getAssignmentPublicId(),
                projection.getWeekPublicId(),
                projection.getWeekStartAt(),
                projection.getConceptPublicId(),
                projection.getConceptTitle(),
                projection.getMediaPublicUrl(),
                projection.getMediaContentType(),
                projection.getOriginalFilename(),
                projection.getCaption(),
                projection.getSubmittedAt(),
                projection.getVisibility()
        );
    }
}
