package com.example.demo.lessonreport.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateLessonReportRequest", description = "Payload for reporting a lesson")
public record CreateLessonReportRequest(
        @Schema(description = "Public UUID of the lesson being reported")
        UUID lessonId,
        @Schema(description = "Learner or contributor provided reason for reporting")
        String reason
) {}
