package com.example.demo.quiz;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.quiz.dto.QuizQuestionAnswerRequest;
import com.example.demo.quiz.dto.SubmitQuizAttemptRequest;

@Component
class QuizAttemptSubmissionValidator {

    QuizAttemptSubmission validate(List<QuizQuestion> questions, SubmitQuizAttemptRequest request) {
        Map<UUID, QuizQuestion> questionsByPublicId = new LinkedHashMap<>();
        for (QuizQuestion question : questions) {
            questionsByPublicId.put(question.getPublicId(), question);
        }

        Map<UUID, QuizQuestionAnswerRequest> answersByQuestionId = new LinkedHashMap<>();
        for (QuizQuestionAnswerRequest answer : request.answers()) {
            if (answer == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer entries are required");
            }
            if (answer.questionPublicId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionPublicId is required");
            }
            if (answersByQuestionId.containsKey(answer.questionPublicId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate answer for question: " + answer.questionPublicId()
                );
            }
            if (!questionsByPublicId.containsKey(answer.questionPublicId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Question does not belong to quiz: " + answer.questionPublicId()
                );
            }
            if (answer.selectedOptionIds() == null || answer.selectedOptionIds().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "selectedOptionIds is required for question: " + answer.questionPublicId()
                );
            }
            answersByQuestionId.put(answer.questionPublicId(), answer);
        }

        if (answersByQuestionId.size() != questionsByPublicId.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission must answer every question exactly once");
        }

        return new QuizAttemptSubmission(answersByQuestionId, questions.size());
    }
}
