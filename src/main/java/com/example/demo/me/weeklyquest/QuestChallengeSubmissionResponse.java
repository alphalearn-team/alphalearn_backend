package com.example.demo.me.weeklyquest;

import java.time.OffsetDateTime;
import java.util.List;
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
        List<QuestChallengeTaggedFriendDto> taggedFriends,
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
                view.taggedFriends(),
                view.submittedAt(),
                view.updatedAt()
        );
    }
}
