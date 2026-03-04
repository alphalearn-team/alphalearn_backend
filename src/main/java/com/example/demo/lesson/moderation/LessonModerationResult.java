package com.example.demo.lesson.moderation;

import java.time.OffsetDateTime;
import java.util.List;

public record LessonModerationResult(
        LessonModerationDecision decision,
        List<String> reasons,
        String providerName,
        Object rawResponse,
        OffsetDateTime completedAt
) {
    public LessonModerationResult {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
