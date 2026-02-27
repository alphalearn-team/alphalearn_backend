package com.example.demo.lesson.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonConceptSummary", description = "Concept reference shown in lesson responses")
public record LessonConceptSummaryDto(
        @Schema(description = "Concept public UUID")
        UUID publicId,
        @Schema(description = "Concept title")
        String title
) {}
