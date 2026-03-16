package com.example.demo.quiz;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.quiz.dto.QuizQuestionAnswerRequest;
import com.fasterxml.jackson.databind.JsonNode;

@Component
class QuizAttemptScoringService {

    int calculateScore(List<QuizQuestion> questions, Map<UUID, QuizQuestionAnswerRequest> answersByQuestionId) {
        int score = 0;
        for (QuizQuestion question : questions) {
            QuizQuestionAnswerRequest answer = answersByQuestionId.get(question.getPublicId());
            if (answer == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission must answer every question exactly once");
            }
            if (isCorrect(question, answer)) {
                score++;
            }
        }
        return score;
    }

    private boolean isCorrect(QuizQuestion question, QuizQuestionAnswerRequest answer) {
        if (question instanceof MCQQuestion mcqQuestion) {
            return isSingleChoiceCorrect(mcqQuestion, answer);
        }
        if (question instanceof MultiSelectQuestion multiSelectQuestion) {
            return isMultiSelectCorrect(multiSelectQuestion, answer);
        }
        if (question instanceof TrueFalseQuestion trueFalseQuestion) {
            return isTrueFalseCorrect(trueFalseQuestion, answer);
        }
        throw new IllegalStateException("Unsupported quiz question type: " + question.getClass().getName());
    }

    private boolean isSingleChoiceCorrect(MCQQuestion question, QuizQuestionAnswerRequest answer) {
        List<String> selectedOptionIds = answer.selectedOptionIds();
        if (selectedOptionIds.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "single-choice questions require exactly one selected option"
            );
        }

        String selectedOptionId = requireValidSingleOptionId(selectedOptionIds.getFirst(), answer.questionPublicId());
        if (!extractOptionIds(question.getOptions()).contains(selectedOptionId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid option selected for question: " + answer.questionPublicId()
            );
        }
        return selectedOptionId.equals(question.getCorrectOptionId());
    }

    private boolean isMultiSelectCorrect(MultiSelectQuestion question, QuizQuestionAnswerRequest answer) {
        Set<String> selectedOptionIds = normalizeSelectedOptionIds(answer.selectedOptionIds(), answer.questionPublicId());
        Set<String> validOptionIds = extractOptionIds(question.getOptions());
        if (!validOptionIds.containsAll(selectedOptionIds)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid option selected for question: " + answer.questionPublicId()
            );
        }
        return selectedOptionIds.equals(extractStringIds(question.getCorrectOptionIds()));
    }

    private boolean isTrueFalseCorrect(TrueFalseQuestion question, QuizQuestionAnswerRequest answer) {
        List<String> selectedOptionIds = answer.selectedOptionIds();
        if (selectedOptionIds.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "true-false questions require exactly one selected option"
            );
        }

        String selectedOptionId = requireValidSingleOptionId(selectedOptionIds.getFirst(), answer.questionPublicId());
        if (!"true".equals(selectedOptionId) && !"false".equals(selectedOptionId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid option selected for question: " + answer.questionPublicId()
            );
        }
        return Boolean.toString(question.isCorrectBoolean()).equals(selectedOptionId);
    }

    private String requireValidSingleOptionId(String optionId, UUID questionPublicId) {
        if (optionId == null || optionId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid option selected for question: " + questionPublicId
            );
        }
        return optionId;
    }

    private Set<String> normalizeSelectedOptionIds(List<String> selectedOptionIds, UUID questionPublicId) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "multiple-choice questions require at least one selected option"
            );
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String optionId : selectedOptionIds) {
            if (optionId == null || optionId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid option selected for question: " + questionPublicId
                );
            }
            normalized.add(optionId);
        }
        return normalized;
    }

    private Set<String> extractOptionIds(JsonNode optionsNode) {
        if (optionsNode == null || !optionsNode.isArray()) {
            throw new IllegalStateException("Quiz question options must be a JSON array.");
        }

        Set<String> optionIds = new LinkedHashSet<>();
        for (JsonNode optionNode : optionsNode) {
            JsonNode idNode = optionNode.get("id");
            if (idNode == null || idNode.asText().isBlank()) {
                throw new IllegalStateException("Quiz question options must have non-blank ids.");
            }
            optionIds.add(idNode.asText());
        }
        return optionIds;
    }

    private Set<String> extractStringIds(JsonNode valuesNode) {
        if (valuesNode == null || !valuesNode.isArray()) {
            throw new IllegalStateException("Quiz answer keys must be a JSON array.");
        }

        Set<String> values = new LinkedHashSet<>();
        for (JsonNode valueNode : valuesNode) {
            String value = valueNode.asText();
            if (value.isBlank()) {
                throw new IllegalStateException("Quiz answer keys must contain non-blank values.");
            }
            values.add(value);
        }
        return values;
    }
}
