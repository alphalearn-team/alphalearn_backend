package com.example.demo.lesson.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateLessonSectionRequest", description = "Request payload for a lesson section")
public record CreateLessonSectionRequest(
        @Schema(description = "Section public UUID (provided when updating existing section, null for new sections)")
        UUID sectionPublicId,
        @Schema(description = "Section type: text, example, callout, definition, or comparison", example = "text")
        String sectionType,
        @Schema(description = "Optional section title", example = "Common Usage")
        String title,
        @Schema(description = "Section content object (structure varies by sectionType)")
        Object content
) {}
