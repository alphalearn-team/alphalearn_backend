package com.example.demo.lesson.admin;

import com.example.demo.lesson.LessonModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminUpdateLessonModerationStatusRequest", description = "Payload for admin moderation status updates")
public record AdminUpdateLessonModerationStatusRequest(
        @Schema(description = "Target moderation status for the lesson")
        LessonModerationStatus status,
        @Schema(description = "When unpublishing, whether to auto-resolve pending reports. Defaults to true.")
        Boolean resolvePendingReports
) {}
