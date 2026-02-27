package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonContributorDetail", description = "Detailed payload for lesson owners, including moderation status")
public record LessonDetailDto(
        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,
        @Schema(description = "Lesson title")
        String title,
        @Schema(description = "Lesson content JSON")
        Object content,
        @Schema(description = "Moderation status text")
        String moderationStatus,
        @Schema(description = "Associated concept public UUIDs")
        List<UUID> conceptPublicIds,
        @Schema(description = "Associated concept summaries")
        List<LessonConceptSummaryDto> concepts,
        @Schema(description = "Lesson author")
        LessonAuthorDto author,
        @Schema(description = "Lesson creation timestamp")
        OffsetDateTime createdAt
) implements LessonDetailView {}
