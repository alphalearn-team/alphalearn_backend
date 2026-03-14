package com.example.demo.me.weeklyquest;

public record QuestChallengeSubmissionRequest(
        String objectKey,
        String originalFilename,
        String caption
) {
    public QuestChallengeSubmissionCommand toCommand() {
        return new QuestChallengeSubmissionCommand(objectKey, originalFilename, caption);
    }
}
