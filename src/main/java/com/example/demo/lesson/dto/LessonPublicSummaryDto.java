package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LessonPublicSummaryDto(
        UUID lessonPublicId,
        String title,
        List<UUID> conceptPublicIds,
        List<LessonConceptSummaryDto> concepts,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) {}
