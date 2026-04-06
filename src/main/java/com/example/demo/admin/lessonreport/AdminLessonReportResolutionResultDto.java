package com.example.demo.admin.lessonreport;

import java.util.UUID;

import com.example.demo.lesson.LessonModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminLessonReportResolutionResult", description = "Result payload after resolving reported lesson entries")
public record AdminLessonReportResolutionResultDto(
        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,
        @Schema(description = "Number of pending reports resolved by this action")
        int resolvedCount,
        @Schema(description = "Current lesson moderation status after action")
        LessonModerationStatus lessonModerationStatus,
        @Schema(description = "Resolution action applied")
        String action
) {}
