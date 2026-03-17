package com.example.demo.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuizOption", description = "Learner-facing quiz answer option")
public record QuizOptionDto(
        @Schema(description = "Stable option identifier")
        String id,

        @Schema(description = "Option text")
        String text
) {}
