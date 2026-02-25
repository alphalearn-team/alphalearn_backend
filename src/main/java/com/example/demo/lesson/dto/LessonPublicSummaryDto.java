package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LessonPublicSummaryDto(
        Integer lessonId,
        String title,
        List<Integer> conceptIds,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) {}
