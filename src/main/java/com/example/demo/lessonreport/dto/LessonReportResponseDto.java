package com.example.demo.lessonreport.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonReport", description = "Response payload after creating a lesson report")
public record LessonReportResponseDto(
        @Schema(description = "Public UUID of the created lesson report")
        UUID reportId,
        @Schema(description = "Public UUID of the reported lesson")
        UUID lessonId,
        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt
) {}
