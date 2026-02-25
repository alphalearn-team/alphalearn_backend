package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LessonDetailDto(
        Integer lessonId,
        String title,
        Object content,
        String moderationStatus,
        List<Integer> conceptIds,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) implements LessonDetailView {}
