package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LessonContributorSummaryDto(
        UUID lessonPublicId,
        String title,
        String moderationStatus,
        List<UUID> conceptPublicIds,
        List<LessonConceptSummaryDto> concepts,
        LessonAuthorDto author,
        OffsetDateTime createdAt
) {}
