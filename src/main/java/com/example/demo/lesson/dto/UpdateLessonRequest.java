package com.example.demo.lesson.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateLessonRequest", description = "Request payload to update lesson title and content")
public record UpdateLessonRequest(
        @Schema(description = "Lesson title")
        String title,
        @Schema(description = "Lesson content object serialized as JSON")
        Object content
) {}
