package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LessonDetailDto(
        UUID lessonPublicId,
        String title,
        Object content,
        String moderationStatus,
        List<UUID> conceptPublicIds,
        List<LessonConceptSummaryDto> concepts,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) implements LessonDetailView {}
