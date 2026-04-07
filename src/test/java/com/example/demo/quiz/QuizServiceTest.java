package com.example.demo.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.read.LessonLookupService;
import com.example.demo.quiz.dto.CreateQuizRequest;
import com.example.demo.quiz.dto.QuizQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private LessonLookupService lessonLookupService;

    private QuizService quizService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepository, lessonLookupService, objectMapper);
    }

    @Test
    void createQuizSuccessfullyWithAllTypes() {
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        ReflectionTestUtils.setField(lesson, "publicId", lessonPublicId);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizQuestionDto mcq = new QuizQuestionDto(
                "single-choice",
                "MCQ Prompt",
                Map.of(
                        "options", List.of(Map.of("id", "1", "text", "A"), Map.of("id", "2", "text", "B")),
                        "correctOptionId", "1"
                )
        );

        QuizQuestionDto multiSelect = new QuizQuestionDto(
                "multiple-choice",
                "Multi Prompt",
                Map.of(
                        "options", List.of(Map.of("id", "1", "text", "A"), Map.of("id", "2", "text", "B")),
                        "correctOptionIds", List.of("1", "2")
                )
        );

        QuizQuestionDto trueFalse = new QuizQuestionDto(
                "true-false",
                "TF Prompt",
                Map.of("correctBoolean", true)
        );

        CreateQuizRequest request = new CreateQuizRequest(lessonPublicId, List.of(mcq, multiSelect, trueFalse));

        Quiz result = quizService.createQuiz(request);

        assertThat(result).isNotNull();
        assertThat(result.getLesson()).isEqualTo(lesson);
        assertThat(result.getQuestions()).hasSize(3);
        assertThat(result.getQuestions().get(0)).isInstanceOf(MCQQuestion.class);
        assertThat(result.getQuestions().get(1)).isInstanceOf(MultiSelectQuestion.class);
        assertThat(result.getQuestions().get(2)).isInstanceOf(TrueFalseQuestion.class);
        
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void createQuizFailsWhenMCQHasInsufficientOptions() {
        UUID lessonPublicId = UUID.randomUUID();
        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(new Lesson());

        QuizQuestionDto invalidMcq = new QuizQuestionDto(
                "single-choice",
                "MCQ Prompt",
                Map.of(
                        "options", List.of(Map.of("id", "1", "text", "A")),
                        "correctOptionId", "1"
                )
        );

        CreateQuizRequest request = new CreateQuizRequest(lessonPublicId, List.of(invalidMcq));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(request));
        assertThat(ex.getMessage()).isEqualTo("MCQ must have at least 2 options.");
    }

    @Test
    void createQuizFailsWhenMultiSelectHasNoCorrectAnswers() {
        UUID lessonPublicId = UUID.randomUUID();
        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(new Lesson());

        QuizQuestionDto invalidMulti = new QuizQuestionDto(
                "multiple-choice",
                "Multi Prompt",
                Map.of(
                        "options", List.of(Map.of("id", "1", "text", "A"), Map.of("id", "2", "text", "B")),
                        "correctOptionIds", List.of()
                )
        );

        CreateQuizRequest request = new CreateQuizRequest(lessonPublicId, List.of(invalidMulti));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(request));
        assertThat(ex.getMessage()).isEqualTo("Multi-select must have at least one correct option.");
    }

    @Test
    void createQuizFailsWhenTypeIsUnknown() {
        UUID lessonPublicId = UUID.randomUUID();
        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(new Lesson());

        QuizQuestionDto unknown = new QuizQuestionDto("unknown-type", "Prompt", Map.of());
        CreateQuizRequest request = new CreateQuizRequest(lessonPublicId, List.of(unknown));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(request));
        assertThat(ex.getMessage()).contains("Unknown question type");
    }
}
