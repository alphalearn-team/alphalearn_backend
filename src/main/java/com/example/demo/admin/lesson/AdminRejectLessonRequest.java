package com.example.demo.admin.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminRejectLessonRequest", description = "Request payload for manually rejecting a lesson in PENDING status")
public record AdminRejectLessonRequest(
        @Schema(
                description = "Required non-blank admin reason for rejecting the lesson",
                example = "Contains unsafe instructions and needs revision before publication."
        )
        String reason
) {}
