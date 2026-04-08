package com.example.demo.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(name = "UpdateQuizRequest", description = "Request payload to replace all questions in an existing quiz")
public record UpdateQuizRequest(
        @Schema(description = "Replacement list of questions; order dictates final display order")
        @Valid
        @NotEmpty
        List<QuizQuestionDto> questions
) {}
