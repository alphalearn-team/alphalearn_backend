package com.example.demo.quiz;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.LessonLookupService;
import com.example.demo.quiz.dto.QuizOptionDto;
import com.example.demo.quiz.dto.QuizQuestionResponseDto;
import com.example.demo.quiz.dto.QuizResponseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QuizQueryService {

    private static final List<QuizOptionDto> TRUE_FALSE_OPTIONS = List.of(
            new QuizOptionDto("true", "True"),
            new QuizOptionDto("false", "False")
    );

    private final QuizRepository quizRepository;
    private final LessonLookupService lessonLookupService;
    private final ObjectMapper objectMapper;

    public QuizQueryService(
            QuizRepository quizRepository,
            LessonLookupService lessonLookupService,
            ObjectMapper objectMapper
    ) {
        this.quizRepository = quizRepository;
        this.lessonLookupService = lessonLookupService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<QuizResponseDto> getQuizzesForLesson(UUID lessonPublicId) {
        lessonLookupService.findByPublicIdOrThrow(lessonPublicId);

        return quizRepository.findByLesson_PublicIdOrderByCreatedAtDesc(lessonPublicId).stream()
                .map(quiz -> new QuizResponseDto(
                        quiz.getPublicId(),
                        lessonPublicId,
                        quiz.getLesson().getContributor().getContributorId(),
                        quiz.getLesson().getTitle(),
                        quiz.getCreatedAt(),
                        quiz.getQuestions().stream()
                                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                                .map(this::toQuestionResponse)
                                .toList()
                ))
                .toList();
    }

    private QuizQuestionResponseDto toQuestionResponse(QuizQuestion question) {
        // Use a more robust check that handles Hibernate proxies if any
        String type = "unknown";
        List<QuizOptionDto> options = List.of();
        List<String> correctIds = List.of();

        if (question instanceof MCQQuestion mcqQuestion) {
            type = "single-choice";
            options = readOptions(mcqQuestion.getOptions());
            if (mcqQuestion.getCorrectOptionId() != null) {
                correctIds = List.of(mcqQuestion.getCorrectOptionId());
            }
        } else if (question instanceof MultiSelectQuestion multiSelectQuestion) {
            type = "multiple-choice";
            options = readOptions(multiSelectQuestion.getOptions());
            correctIds = extractIdsFromJson(multiSelectQuestion.getCorrectOptionIds());
        } else if (question instanceof TrueFalseQuestion trueFalseQuestion) {
            type = "true-false";
            options = TRUE_FALSE_OPTIONS;
            correctIds = List.of(Boolean.toString(trueFalseQuestion.isCorrectBoolean()));
        }

        return new QuizQuestionResponseDto(
                question.getPublicId(),
                type,
                question.getPrompt(),
                question.getOrderIndex(),
                options,
                correctIds
        );
    }

    private List<String> extractIdsFromJson(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                ids.add(item.asText());
            } else if (item.isObject() && item.has("id")) {
                ids.add(item.get("id").asText());
            } else {
                ids.add(item.asText());
            }
        }
        return ids;
    }

    private List<QuizOptionDto> readOptions(JsonNode optionsNode) {
        if (optionsNode == null || !optionsNode.isArray()) {
            throw new IllegalStateException("Quiz question options must be a JSON array.");
        }

        try {
            return objectMapper.convertValue(
                    optionsNode,
                    new TypeReference<List<QuizOptionDto>>() {
                    }
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Quiz question options could not be converted.", ex);
        }
    }
}
