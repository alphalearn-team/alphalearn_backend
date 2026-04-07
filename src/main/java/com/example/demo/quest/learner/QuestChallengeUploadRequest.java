package com.example.demo.quest.learner;

public record QuestChallengeUploadRequest(
        String filename,
        String contentType,
        Long fileSizeBytes
) {}
