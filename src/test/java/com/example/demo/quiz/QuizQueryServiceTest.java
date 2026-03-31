package com.example.demo.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.contributor.Contributor;
import com.example.demo.quiz.dto.QuizQuestionResponseDto;
import com.example.demo.quiz.dto.QuizResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class QuizQueryServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private LessonLookupService lessonLookupService;

    @Mock
    private com.example.demo.lessonenrollment.LessonEnrollmentService lessonEnrollmentService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private QuizQueryService quizQueryService;

    @BeforeEach
    void setUp() {
        quizQueryService = new QuizQueryService(quizRepository, lessonLookupService, lessonEnrollmentService, objectMapper);
    }

    @Test
    void getQuizzesForLessonReturnsLearnerSafeDtosWithOrderedQuizzesAndQuestions() throws Exception {
        UUID lessonPublicId = UUID.randomUUID();
        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(new Lesson());
        when(lessonEnrollmentService.isEnrolled(any(), any())).thenReturn(true);

        Quiz olderQuiz = quiz(
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-03-15T09:00:00Z"),
                questionSetForOlderQuiz()
        );
        Quiz newerQuiz = quiz(
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-03-16T09:00:00Z"),
                questionSetForNewerQuiz()
        );

        when(quizRepository.findByLesson_PublicIdOrderByCreatedAtDesc(lessonPublicId))
                .thenReturn(List.of(newerQuiz, olderQuiz));

        List<QuizResponseDto> result = quizQueryService.getQuizzesForLesson(lessonPublicId, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).quizPublicId()).isEqualTo(newerQuiz.getPublicId());
        assertThat(result.get(1).quizPublicId()).isEqualTo(olderQuiz.getPublicId());
        assertThat(result).extracting(QuizResponseDto::lessonPublicId).containsOnly(lessonPublicId);
        assertThat(result).extracting(QuizResponseDto::lessonTitle).containsOnly("Test Lesson");
        assertThat(result).extracting(QuizResponseDto::canAttempt).containsOnly(true);

        List<QuizQuestionResponseDto> newerQuestions = result.get(0).questions();
        assertThat(newerQuestions).extracting(QuizQuestionResponseDto::orderIndex).containsExactly(0, 1, 2);
        assertThat(newerQuestions).extracting(QuizQuestionResponseDto::type)
                .containsExactly("single-choice", "multiple-choice", "true-false");

        assertThat(newerQuestions.get(0).options())
                .extracting(option -> option.text())
                .containsExactly("3", "4");
        assertThat(newerQuestions.get(0).correctAnswerIds()).containsExactly("b");

        assertThat(newerQuestions.get(1).options())
                .extracting(option -> option.text())
                .containsExactly("2", "3", "5");
        assertThat(newerQuestions.get(1).correctAnswerIds()).containsExactly("a", "b", "c");

        assertThat(newerQuestions.get(2).options())
                .extracting(option -> option.id() + ":" + option.text())
                .containsExactly("true:True", "false:False");
        assertThat(newerQuestions.get(2).correctAnswerIds()).containsExactly("true");

        String json = objectMapper.writeValueAsString(result);
        assertThat(json).contains("correctAnswerIds");
        assertThat(json).doesNotContain("correctOptionId");
        assertThat(json).doesNotContain("correctOptionIds");
        assertThat(json).doesNotContain("correctBoolean");

        verify(lessonLookupService).findByPublicIdOrThrow(lessonPublicId);
        verify(quizRepository).findByLesson_PublicIdOrderByCreatedAtDesc(lessonPublicId);
    }

    @Test
    void getQuizzesForLessonReturnsEmptyListWhenLessonHasNoQuizzes() {
        UUID lessonPublicId = UUID.randomUUID();
        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(new Lesson());
        when(lessonEnrollmentService.isEnrolled(any(), any())).thenReturn(true);
        when(quizRepository.findByLesson_PublicIdOrderByCreatedAtDesc(lessonPublicId)).thenReturn(List.of());

        List<QuizResponseDto> result = quizQueryService.getQuizzesForLesson(lessonPublicId, null);

        assertThat(result).isEmpty();
    }

    private Quiz quiz(UUID quizPublicId, OffsetDateTime createdAt, List<QuizQuestion> questions) {
        Lesson lesson = new Lesson();
        lesson.setTitle("Test Lesson");
        Contributor contributor = new Contributor();
        // Use a fixed UUID for consistency in tests
        contributor.setContributorId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        lesson.setContributor(contributor);
        
        Quiz quiz = new Quiz(lesson, createdAt);
        ReflectionTestUtils.setField(quiz, "publicId", quizPublicId);
        quiz.setQuestions(new ArrayList<>(questions));
        quiz.getQuestions().forEach(question -> question.setQuiz(quiz));
        return quiz;
    }

    private List<QuizQuestion> questionSetForNewerQuiz() {
        Lesson lesson = new Lesson();

        TrueFalseQuestion question2 = new TrueFalseQuestion(lessonQuiz(lesson), "The sky is blue.", 2, true);
        ReflectionTestUtils.setField(question2, "publicId", UUID.randomUUID());

        MultiSelectQuestion question1 = new MultiSelectQuestion(
                lessonQuiz(lesson),
                "Pick primes.",
                1,
                objectMapper.valueToTree(List.of(
                        java.util.Map.of("id", "a", "text", "2"),
                        java.util.Map.of("id", "b", "text", "3"),
                        java.util.Map.of("id", "c", "text", "5")
                )),
                objectMapper.valueToTree(List.of("a", "b", "c"))
        );
        ReflectionTestUtils.setField(question1, "publicId", UUID.randomUUID());

        MCQQuestion question0 = new MCQQuestion(
                lessonQuiz(lesson),
                "What is 2 + 2?",
                0,
                objectMapper.valueToTree(List.of(
                        java.util.Map.of("id", "a", "text", "3"),
                        java.util.Map.of("id", "b", "text", "4")
                )),
                "b"
        );
        ReflectionTestUtils.setField(question0, "publicId", UUID.randomUUID());

        return List.of(question2, question1, question0);
    }

    private List<QuizQuestion> questionSetForOlderQuiz() {
        Lesson lesson = new Lesson();
        MCQQuestion question = new MCQQuestion(
                lessonQuiz(lesson),
                "Older question",
                0,
                objectMapper.valueToTree(List.of(
                        java.util.Map.of("id", "a", "text", "Old A"),
                        java.util.Map.of("id", "b", "text", "Old B")
                )),
                "a"
        );
        ReflectionTestUtils.setField(question, "publicId", UUID.randomUUID());
        return List.of(question);
    }

    private Quiz lessonQuiz(Lesson lesson) {
        return new Quiz(lesson, OffsetDateTime.now());
    }
}
