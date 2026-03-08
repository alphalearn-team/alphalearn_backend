package com.example.demo.admin.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lesson.dto.LessonSectionDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminLessonReview", description = "Admin detail payload used during moderation review, including moderation outcomes from history.")
public record AdminLessonReviewDto(
        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,
        @Schema(description = "Lesson title")
        String title,
        @Schema(description = "Lesson content object (legacy field, may be null for section-based lessons)")
        Object content,
        @Schema(description = "Associated concept public UUIDs")
        List<UUID> conceptPublicIds,
        @Schema(description = "Lesson author")
        LessonAuthorDto author,
        @Schema(description = "Current lesson moderation status")
        LessonModerationStatus lessonModerationStatus,
        @Schema(description = "Latest automated moderation reasons for this lesson; empty when none were recorded")
        List<String> automatedModerationReasons,
        @Schema(description = "Latest admin rejection reason, present only when the latest admin moderation action was a rejection")
        String adminRejectionReason,
        @Schema(description = "Lesson creation timestamp")
        OffsetDateTime createdAt,
        @Schema(description = "Soft-delete timestamp; null means active")
        OffsetDateTime deletedAt,
        @Schema(description = "Lesson sections (ordered by orderIndex)")
        List<LessonSectionDto> sections,
        @Schema(description = "Total number of sections")
        Integer totalSections
) {}
