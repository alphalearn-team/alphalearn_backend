package com.example.demo.quiz.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuizAttemptResponse", description = "Learner quiz attempt summary")
public record QuizAttemptResponse(
        @Schema(description = "Quiz public UUID")
        UUID quizPublicId,

        @Schema(description = "Attempt timestamp")
        OffsetDateTime attemptedAt,

        @Schema(description = "Number of correctly answered questions")
        int score,

        @Schema(description = "Total number of questions in the quiz")
        int totalQuestions,

        @Schema(description = "Whether this is the learner's first attempt for the quiz")
        boolean isFirstAttempt
) {}
