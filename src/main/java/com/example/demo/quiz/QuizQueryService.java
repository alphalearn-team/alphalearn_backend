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
                        quiz.getCreatedAt(),
                        quiz.getQuestions().stream()
                                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                                .map(this::toQuestionResponse)
                                .toList()
                ))
                .toList();
    }

    private QuizQuestionResponseDto toQuestionResponse(QuizQuestion question) {
        if (question instanceof MCQQuestion mcqQuestion) {
            return new QuizQuestionResponseDto(
                    question.getPublicId(),
                    "single-choice",
                    question.getPrompt(),
                    question.getOrderIndex(),
                    readOptions(mcqQuestion.getOptions())
            );
        }

        if (question instanceof MultiSelectQuestion multiSelectQuestion) {
            return new QuizQuestionResponseDto(
                    question.getPublicId(),
                    "multiple-choice",
                    question.getPrompt(),
                    question.getOrderIndex(),
                    readOptions(multiSelectQuestion.getOptions())
            );
        }

        if (question instanceof TrueFalseQuestion) {
            return new QuizQuestionResponseDto(
                    question.getPublicId(),
                    "true-false",
                    question.getPrompt(),
                    question.getOrderIndex(),
                    TRUE_FALSE_OPTIONS
            );
        }

        throw new IllegalStateException("Unsupported quiz question type: " + question.getClass().getName());
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
