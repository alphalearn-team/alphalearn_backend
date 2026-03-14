package com.example.demo.me.weeklyquest;

public record QuestChallengeSubmissionCommand(
        String objectKey,
        String originalFilename,
        String caption
) {}
