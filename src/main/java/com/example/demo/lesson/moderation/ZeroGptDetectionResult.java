package com.example.demo.lesson.moderation;

public record ZeroGptDetectionResult(
        double aiProbability,
        boolean shouldReject,
        String reason,
        Object rawResponse
) {
}
