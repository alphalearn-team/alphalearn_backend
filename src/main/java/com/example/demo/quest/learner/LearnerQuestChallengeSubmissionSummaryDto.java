package com.example.demo.quest.learner;

import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmission;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LearnerQuestChallengeSubmissionSummaryDto(
        UUID publicId,
        String objectKey,
        String publicUrl,
        String contentType,
        String originalFilename,
        long fileSizeBytes,
        String caption,
        OffsetDateTime submittedAt,
        OffsetDateTime updatedAt
) {
    public static LearnerQuestChallengeSubmissionSummaryDto from(WeeklyQuestChallengeSubmission submission) {
        return new LearnerQuestChallengeSubmissionSummaryDto(
                submission.getPublicId(),
                submission.getMediaObjectKey(),
                submission.getMediaPublicUrl(),
                submission.getMediaContentType(),
                submission.getOriginalFilename(),
                submission.getFileSizeBytes(),
                submission.getCaption(),
                submission.getSubmittedAt(),
                submission.getUpdatedAt()
        );
    }
}
