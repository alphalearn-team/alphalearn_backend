package com.example.demo.lesson.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonPublicDetail", description = "Detailed payload for public lesson view")
public record LessonPublicDetailDto(
        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,
        @Schema(description = "Lesson title")
        String title,
        @Schema(description = "Lesson content JSON (legacy field, may be null for section-based lessons)")
        Object content,
        @Schema(description = "Associated concept public UUIDs")
        List<UUID> conceptPublicIds,
        @Schema(description = "Associated concept summaries")
        List<LessonConceptSummaryDto> concepts,
        @Schema(description = "Lesson author")
        LessonAuthorDto author,
        @Schema(description = "Lesson creation timestamp")
        OffsetDateTime createdAt,
        @Schema(description = "Lesson sections (ordered by orderIndex)")
        List<LessonSectionDto> sections,
        @Schema(description = "Total number of sections")
        Integer totalSections
) implements LessonDetailView {}