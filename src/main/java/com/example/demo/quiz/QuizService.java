package com.example.demo.quiz;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.quiz.dto.CreateQuizRequest;
import com.example.demo.quiz.dto.QuizQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;

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

            quiz.getQuestions().add(question);
        }

        return quizRepository.save(quiz);
    }
}
