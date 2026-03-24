package com.example.demo.quiz.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuizResponse", description = "Learner-safe quiz payload")
public record QuizResponseDto(
        @Schema(description = "Quiz public UUID")
        UUID quizPublicId,

        @Schema(description = "Lesson public UUID")
        UUID lessonPublicId,

        @Schema(description = "Contributor user UUID (lesson owner)")
        UUID contributorId,

        @Schema(description = "Lesson title")
        String lessonTitle,

        @Schema(description = "Quiz creation timestamp")
        OffsetDateTime createdAt,

        @Schema(description = "Questions in display order")
        List<QuizQuestionResponseDto> questions
) {}
