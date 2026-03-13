package com.example.demo.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "QuizQuestionDto", description = "Payload for individual quiz questions")
public record QuizQuestionDto(
        @Schema(description = "Question type", example = "multiple-choice")
        @NotBlank
        String type,

        @Schema(description = "Question prompt", example = "What is 2+2?")
        @NotBlank
        String prompt,

        @Schema(description = "Type-specific properties like options and correct answers")
        @NotNull
        JsonNode properties
) {}
