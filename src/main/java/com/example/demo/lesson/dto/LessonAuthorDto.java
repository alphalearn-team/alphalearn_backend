package com.example.demo.lesson.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonAuthor", description = "Public author info embedded in lesson responses")
public record LessonAuthorDto(
        @Schema(description = "Author learner public UUID")
        UUID publicId,
        @Schema(description = "Author username")
        String username
) {}
