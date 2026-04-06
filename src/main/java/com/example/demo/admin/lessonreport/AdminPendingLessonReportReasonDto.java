package com.example.demo.admin.lessonreport;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminPendingLessonReportReason", description = "Pending report entry shown in admin reported lesson detail")
public record AdminPendingLessonReportReasonDto(
        @Schema(description = "Lesson report public UUID")
        UUID reportId,
        @Schema(description = "Submitted report reason")
        String reason,
        @Schema(description = "When the lesson was reported")
        OffsetDateTime reportedAt
) {}
