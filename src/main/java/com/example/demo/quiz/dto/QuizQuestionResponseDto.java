package com.example.demo.quiz.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuizQuestionResponse", description = "Learner-safe quiz question payload")
public record QuizQuestionResponseDto(
        @Schema(description = "Question public UUID")
        UUID questionPublicId,

        @Schema(description = "Question type", example = "single-choice")
        String type,

        @Schema(description = "Question prompt")
        String prompt,

        @Schema(description = "Display order within the quiz")
        int orderIndex,

        @Schema(description = "Options visible to learners")
        List<QuizOptionDto> options
) {}
