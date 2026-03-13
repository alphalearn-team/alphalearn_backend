package com.example.demo.quiz.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

@Schema(name = "CreateQuizRequest", description = "Request payload to create a new quiz linked to a lesson")
public record CreateQuizRequest(
        @Schema(description = "The public UUID of the lesson this quiz belongs to")
        @NotNull
        UUID lessonPublicId,

        @Schema(description = "The title of the quiz", example = "Chapter 1 Review")
        @NotBlank
        String title,

        @Schema(description = "Optional description or instructions for the quiz")
        String description,

        @Schema(description = "List of questions in the quiz; order dictates the final display order")
        @Valid
        @NotEmpty
        List<QuizQuestionDto> questions
) {}
