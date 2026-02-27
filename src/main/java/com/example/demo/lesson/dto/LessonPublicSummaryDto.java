package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonPublicSummary", description = "Summary payload for public lesson listing")
public record LessonPublicSummaryDto(
        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,
        @Schema(description = "Lesson title")
        String title,
        @Schema(description = "Associated concept public UUIDs")
        List<UUID> conceptPublicIds,
        @Schema(description = "Associated concept summaries")
        List<LessonConceptSummaryDto> concepts,
        @Schema(description = "Lesson author")
        LessonAuthorDto author,
        @Schema(description = "Lesson creation timestamp")
        OffsetDateTime createdAt
) {}
