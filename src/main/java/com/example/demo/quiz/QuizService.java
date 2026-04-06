package com.example.demo.quiz;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.read.LessonLookupService;
import com.example.demo.quiz.dto.CreateQuizRequest;
import com.example.demo.quiz.dto.QuizQuestionDto;
import com.example.demo.quiz.dto.UpdateQuizRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final LessonLookupService lessonLookupService;
    private final ObjectMapper objectMapper;

    public QuizService(QuizRepository quizRepository, LessonLookupService lessonLookupService, ObjectMapper objectMapper) {
        this.quizRepository = quizRepository;
        this.lessonLookupService = lessonLookupService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Quiz createQuiz(CreateQuizRequest request) {
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(request.lessonPublicId());

        Quiz quiz = new Quiz(
                lesson,
                java.time.OffsetDateTime.now());

        List<QuizQuestionDto> requestedQuestions = request.questions();
        for (int i = 0; i < requestedQuestions.size(); i++) {
            QuizQuestionDto dto = requestedQuestions.get(i);
            Map<String, Object> props = dto.properties();
            
            QuizQuestion question;
            if ("multiple-choice".equals(dto.type())) {
                com.fasterxml.jackson.databind.JsonNode options = objectMapper.valueToTree(props.get("options"));
                com.fasterxml.jackson.databind.JsonNode correctOptionIds = objectMapper.valueToTree(props.get("correctOptionIds"));
                question = new MultiSelectQuestion(
                        quiz,
                        dto.prompt(),
                        i,
                        options,
                        correctOptionIds
                );
            } else if ("single-choice".equals(dto.type())) {
                com.fasterxml.jackson.databind.JsonNode options = objectMapper.valueToTree(props.get("options"));
                String correctOptionId = (String) props.get("correctOptionId");
                question = new MCQQuestion(
                        quiz,
                        dto.prompt(),
                        i,
                        options,
                        correctOptionId
                );
            } else if ("true-false".equals(dto.type())) {
                boolean correctBoolean = (Boolean) props.get("correctBoolean");
                question = new TrueFalseQuestion(
                        quiz,
                        dto.prompt(),
                        i,
                        correctBoolean
                );
            } else {
                throw new IllegalArgumentException("Unknown question type: " + dto.type());
            }

            validateQuestionSpecifics(dto);

            quiz.getQuestions().add(question);
        }

        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz updateQuiz(UUID quizPublicId, UpdateQuizRequest request) {
        Quiz quiz = quizRepository.findByPublicId(quizPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        quiz.getQuestions().clear();

        List<QuizQuestionDto> requestedQuestions = request.questions();
        for (int i = 0; i < requestedQuestions.size(); i++) {
            QuizQuestionDto dto = requestedQuestions.get(i);
            Map<String, Object> props = dto.properties();

            QuizQuestion question;
            if ("multiple-choice".equals(dto.type())) {
                com.fasterxml.jackson.databind.JsonNode options = objectMapper.valueToTree(props.get("options"));
                com.fasterxml.jackson.databind.JsonNode correctOptionIds = objectMapper.valueToTree(props.get("correctOptionIds"));
                question = new MultiSelectQuestion(quiz, dto.prompt(), i, options, correctOptionIds);
            } else if ("single-choice".equals(dto.type())) {
                com.fasterxml.jackson.databind.JsonNode options = objectMapper.valueToTree(props.get("options"));
                String correctOptionId = (String) props.get("correctOptionId");
                question = new MCQQuestion(quiz, dto.prompt(), i, options, correctOptionId);
            } else if ("true-false".equals(dto.type())) {
                boolean correctBoolean = (Boolean) props.get("correctBoolean");
                question = new TrueFalseQuestion(quiz, dto.prompt(), i, correctBoolean);
            } else {
                throw new IllegalArgumentException("Unknown question type: " + dto.type());
            }

            validateQuestionSpecifics(dto);
            quiz.getQuestions().add(question);
        }

        return quizRepository.save(quiz);
    }

    private void validateQuestionSpecifics(QuizQuestionDto dto) {
        Map<String, Object> props = dto.properties();
        String type = dto.type();

        if ("multiple-choice".equals(type)) {
            List<?> options = (List<?>) props.get("options");
            List<?> correctIds = (List<?>) props.get("correctOptionIds");
            if (options == null || options.size() < 2) {
                throw new IllegalArgumentException("Multi-select must have at least 2 options.");
            }
            if (correctIds == null || correctIds.isEmpty()) {
                throw new IllegalArgumentException("Multi-select must have at least one correct option.");
            }
        } else if ("single-choice".equals(type)) {
            List<?> options = (List<?>) props.get("options");
            String correctId = (String) props.get("correctOptionId");
            if (options == null || options.size() < 2) {
                throw new IllegalArgumentException("MCQ must have at least 2 options.");
            }
            if (correctId == null || correctId.isBlank()) {
                throw new IllegalArgumentException("MCQ must have a correct option selected.");
            }
        }
    }
}
