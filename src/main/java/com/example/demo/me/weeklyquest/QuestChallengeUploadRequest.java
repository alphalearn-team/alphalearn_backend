package com.example.demo.me.weeklyquest;

public record QuestChallengeUploadRequest(
        String filename,
        String contentType,
        Long fileSizeBytes
) {}
