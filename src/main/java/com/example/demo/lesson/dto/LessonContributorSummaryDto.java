package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LessonContributorSummaryDto(
        Integer lessonId,
        String title,
        String moderationStatus,
        List<Integer> conceptIds,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) {}
