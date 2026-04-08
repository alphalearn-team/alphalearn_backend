package com.example.demo.lesson.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminLessonModerationActionRequest", description = "Payload for admin lesson moderation actions")
public record AdminLessonModerationActionRequest(
        @Schema(description = "Supported values: APPROVE, REJECT, UNPUBLISH")
        String action,
        @Schema(description = "Required when action is REJECT")
        String reason,
        @Schema(description = "For UNPUBLISH, whether to auto-resolve pending reports. Defaults to true.")
        Boolean resolvePendingReports
) {}
