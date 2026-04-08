package com.example.demo.lesson.read;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LessonPublicationActionRequest", description = "Payload for lesson publication state actions")
public record LessonPublicationActionRequest(
        @Schema(description = "Supported values: SUBMIT, UNPUBLISH")
        String action
) {}
