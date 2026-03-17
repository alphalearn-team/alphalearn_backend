package com.example.demo.quiz.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SubmitQuizAttemptRequest", description = "Learner quiz submission payload")
public record SubmitQuizAttemptRequest(
        @Schema(description = "Submitted answers, one per quiz question")
        List<QuizQuestionAnswerRequest> answers
) {}
