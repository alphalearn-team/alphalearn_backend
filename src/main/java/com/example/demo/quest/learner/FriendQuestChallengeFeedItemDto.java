package com.example.demo.quest.learner;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.quest.weekly.FriendQuestChallengeFeedProjection;

public record FriendQuestChallengeFeedItemDto(
        UUID submissionPublicId,
        UUID learnerPublicId,
        String learnerUsername,
        UUID conceptPublicId,
        String conceptTitle,
        UUID assignmentPublicId,
        String mediaPublicUrl,
        String mediaContentType,
        String originalFilename,
        String caption,
        OffsetDateTime submittedAt
) {
    public static FriendQuestChallengeFeedItemDto from(FriendQuestChallengeFeedProjection projection) {
        return new FriendQuestChallengeFeedItemDto(
                projection.getSubmissionPublicId(),
                projection.getLearnerPublicId(),
                projection.getLearnerUsername(),
                projection.getConceptPublicId(),
                projection.getConceptTitle(),
                projection.getAssignmentPublicId(),
                projection.getMediaPublicUrl(),
                projection.getMediaContentType(),
                projection.getOriginalFilename(),
                projection.getCaption(),
                projection.getSubmittedAt()
        );
    }
}
