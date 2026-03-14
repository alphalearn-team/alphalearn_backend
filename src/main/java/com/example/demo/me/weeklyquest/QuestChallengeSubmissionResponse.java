package com.example.demo.me.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QuestChallengeSubmissionResponse(
        UUID publicId,
        UUID assignmentPublicId,
        String objectKey,
        String publicUrl,
        String contentType,
        String originalFilename,
        long fileSizeBytes,
        String caption,
        OffsetDateTime submittedAt,
        OffsetDateTime updatedAt
) {
    public static QuestChallengeSubmissionResponse from(QuestChallengeSubmissionView view) {
        return new QuestChallengeSubmissionResponse(
                view.publicId(),
                view.assignmentPublicId(),
                view.objectKey(),
                view.publicUrl(),
                view.contentType(),
                view.originalFilename(),
                view.fileSizeBytes(),
                view.caption(),
                view.submittedAt(),
                view.updatedAt()
        );
    }
}
