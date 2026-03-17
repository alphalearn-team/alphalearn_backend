package com.example.demo.quiz.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuizQuestionAnswerRequest", description = "Learner answer for a quiz question")
public record QuizQuestionAnswerRequest(
        @Schema(description = "Question public UUID")
        UUID questionPublicId,

        @Schema(description = "Selected option IDs from the learner-safe quiz payload")
        List<String> selectedOptionIds
) {}
