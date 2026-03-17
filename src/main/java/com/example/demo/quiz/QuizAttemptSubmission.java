package com.example.demo.quiz;

import java.util.Map;
import java.util.UUID;

import com.example.demo.quiz.dto.QuizQuestionAnswerRequest;

record QuizAttemptSubmission(
        Map<UUID, QuizQuestionAnswerRequest> answersByQuestionId,
        int totalQuestions
) {}
