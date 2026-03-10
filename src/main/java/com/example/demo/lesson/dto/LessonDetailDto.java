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
        @Schema(description = "Lesson content JSON (legacy field, may be null for section-based lessons)")
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
        OffsetDateTime createdAt,
        @Schema(description = "Latest moderation reasons for the lesson owner")
        List<String> latestModerationReasons,
        @Schema(description = "Latest moderation event type")
        String latestModerationEventType,
        @Schema(description = "Timestamp of latest moderation event")
        OffsetDateTime latestModeratedAt,
        @Schema(description = "Latest admin rejection reason for the lesson owner, if the most recent admin moderation action was a rejection")
        String adminRejectionReason,
        @Schema(description = "Lesson sections (ordered by orderIndex)")
        List<LessonSectionDto> sections,
        @Schema(description = "Total number of sections")
        Integer totalSections
) implements LessonDetailView {}
