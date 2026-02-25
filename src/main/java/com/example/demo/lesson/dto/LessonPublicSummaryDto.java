package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LessonPublicSummaryDto(
        Integer lessonId,
        String title,
        List<Integer> conceptIds,
        UUID contributorId,
        OffsetDateTime createdAt
) {}
