package com.example.demo.admin.lessonreport;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminReportedLessonSummary", description = "Admin summary row for a lesson with pending reports")
public record AdminReportedLessonSummaryDto(
        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,
        @Schema(description = "Lesson title")
        String title,
        @Schema(description = "Lesson author")
        LessonAuthorDto author,
        @Schema(description = "Current lesson moderation status")
        LessonModerationStatus lessonModerationStatus,
        @Schema(description = "Count of pending reports for this lesson")
        long pendingReportCount,
        @Schema(description = "Latest pending report reason")
        String latestReason,
        @Schema(description = "Timestamp of the latest pending report")
        OffsetDateTime latestReportedAt
) {}
