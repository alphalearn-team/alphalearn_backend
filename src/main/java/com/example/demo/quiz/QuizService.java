package com.example.demo.quiz;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.quiz.dto.CreateQuizRequest;
import com.example.demo.quiz.dto.QuizQuestionDto;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final LessonLookupService lessonLookupService;

    public QuizService(QuizRepository quizRepository, LessonLookupService lessonLookupService) {
        this.quizRepository = quizRepository;
        this.lessonLookupService = lessonLookupService;
    }

    @Transactional
    public Quiz createQuiz(CreateQuizRequest request) {
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(request.lessonPublicId());

        Quiz quiz = new Quiz(
                lesson,
                request.title(),
                request.description(),
                java.time.OffsetDateTime.now()
        );

        List<QuizQuestionDto> requestedQuestions = request.questions();
        for (int i = 0; i < requestedQuestions.size(); i++) {
            QuizQuestionDto dto = requestedQuestions.get(i);
            QuizQuestion question = new QuizQuestion(
                    quiz,
                    dto.type(),
                    dto.prompt(),
                    i, // Maintain order index explicitly
                    dto.properties()
            );
            quiz.getQuestions().add(question);
        }

        return quizRepository.save(quiz);
    }
}
