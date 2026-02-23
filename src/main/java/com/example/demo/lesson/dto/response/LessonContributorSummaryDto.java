package com.example.demo.lesson.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LessonContributorSummaryDto(
        Integer lessonId,
        String title,
        String moderationStatus,
        List<Integer> conceptIds,
        UUID contributorId,
        OffsetDateTime createdAt
) {}
