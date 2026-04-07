package com.example.demo.quest.learner;

import java.util.List;
import java.util.UUID;

public record QuestChallengeSubmissionRequest(
        String objectKey,
        String originalFilename,
        String caption,
        List<UUID> taggedFriendPublicIds
) {
    public QuestChallengeSubmissionCommand toCommand() {
        return new QuestChallengeSubmissionCommand(objectKey, originalFilename, caption, taggedFriendPublicIds);
    }
}
