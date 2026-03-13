package com.example.demo.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(name = "QuizQuestionDto", description = "Payload for individual quiz questions")
public record QuizQuestionDto(
        @Schema(description = "Question type", example = "multiple-choice")
        @NotBlank
        String type,

        @Schema(description = "Question prompt", example = "What is 2+2?")
        @NotBlank
        String prompt,

        @Schema(
                description = "Type-specific properties. \n\n" +
                        "For 'multiple-choice': \n" +
                        "```json\n" +
                        "{\n" +
                        "  \"options\": [\n" +
                        "    { \"id\": \"uuid\", \"text\": \"Option A\" }\n" +
                        "  ],\n" +
                        "  \"correctOptionIds\": [\"uuid\"]\n" +
                        "}\n" +
                        "```\n\n" +
                        "For 'true-false': \n" +
                        "```json\n" +
                        "{\n" +
                        "  \"correctBoolean\": true\n" +
                        "}\n" +
                        "```"
        )
        @NotNull
        Map<String, Object> properties
) {}
