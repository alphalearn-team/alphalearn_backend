package com.example.demo.lesson.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateLessonRequest", description = "Request payload to create a lesson")
public record CreateLessonRequest(
        @Schema(description = "Lesson title", example = "Introduction to Linear Equations")
        String title,
        @Schema(description = "Lesson content object serialized as JSON (legacy field, optional if sections provided)")
        Object content,
        @Schema(description = "Concept public UUIDs linked to this lesson")
        List<UUID> conceptPublicIds,
        @Schema(description = "If true, create directly as PENDING; otherwise UNPUBLISHED")
        Boolean submit,
        @Schema(description = "Array of lesson sections (new section-based format)")
        List<CreateLessonSectionRequest> sections
) {}
