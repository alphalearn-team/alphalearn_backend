package com.example.demo.lesson.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonSectionDto", description = "Lesson section detail")
public record LessonSectionDto(
        @Schema(description = "Section public UUID")
        UUID sectionPublicId,
        @Schema(description = "Zero-indexed position in lesson", example = "0")
        Integer orderIndex,
        @Schema(description = "Section type", example = "text")
        String sectionType,
        @Schema(description = "Optional section title", example = "Common Usage")
        String title,
        @Schema(description = "Section content object (structure varies by sectionType)")
        Object content
) {}
