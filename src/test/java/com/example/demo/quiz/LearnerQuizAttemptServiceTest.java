package com.example.demo.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.quiz.dto.QuizAttemptResponse;
import com.example.demo.quiz.dto.QuizQuestionAnswerRequest;
import com.example.demo.quiz.dto.SubmitQuizAttemptRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LearnerQuizAttemptServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private com.example.demo.lesson.enrollment.LessonEnrollmentService lessonEnrollmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LearnerQuizAttemptService learnerQuizAttemptService;

    @BeforeEach
    void setUp() {
        learnerQuizAttemptService = new LearnerQuizAttemptService(
                quizRepository,
                quizAttemptRepository,
                new QuizAttemptSubmissionValidator(),
                new QuizAttemptScoringService(),
                lessonEnrollmentService
        );
    }

    @Test
    void submitQuizAttemptScoresMixedQuestionTypesAndMarksFirstAttempt() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.existsByLearner_IdAndQuiz_QuizId(learnerUser.userId(), quiz.getQuizId())).thenReturn(false);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        QuizAttemptResponse response = learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser);

        assertThat(response.quizPublicId()).isEqualTo(quiz.getPublicId());
        assertThat(response.score()).isEqualTo(3);
        assertThat(response.totalQuestions()).isEqualTo(3);
        assertThat(response.isFirstAttempt()).isTrue();
        assertThat(response.attemptedAt()).isNotNull();

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getLearner()).isEqualTo(learnerUser.learner());
        assertThat(captor.getValue().getQuiz()).isEqualTo(quiz);
        assertThat(captor.getValue().getScore()).isEqualTo((short) 3);
        assertThat(captor.getValue().isFirstAttempt()).isTrue();
    }

    @Test
    void submitQuizAttemptMarksLaterAttemptsAsNotFirstAndPersistsEachSubmission() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.existsByLearner_IdAndQuiz_QuizId(learnerUser.userId(), quiz.getQuizId()))
                .thenReturn(false, true);

        SubmitQuizAttemptRequest firstRequest = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));
        SubmitQuizAttemptRequest secondRequest = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-a")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("false"))
        ));

        QuizAttemptResponse first = learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), firstRequest, learnerUser);
        QuizAttemptResponse second = learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), secondRequest, learnerUser);

        assertThat(first.isFirstAttempt()).isTrue();
        assertThat(second.isFirstAttempt()).isFalse();
        assertThat(second.score()).isZero();
        verify(quizAttemptRepository, times(2)).save(any(QuizAttempt.class));
    }

    @Test
    void getLatestQuizAttemptReturnsMostRecentAttempt() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();
        QuizAttempt latestAttempt = new QuizAttempt(
                quiz,
                learnerUser.learner(),
                (short) 2,
                false,
                OffsetDateTime.parse("2026-03-16T13:00:00Z")
        );

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.findFirstByLearner_IdAndQuiz_QuizIdOrderByAttemptedAtDescAttemptIdDesc(
                learnerUser.userId(),
                quiz.getQuizId()
        )).thenReturn(java.util.Optional.of(latestAttempt));

        QuizAttemptResponse response = learnerQuizAttemptService.getLatestQuizAttempt(quiz.getPublicId(), learnerUser);

        assertThat(response.quizPublicId()).isEqualTo(quiz.getPublicId());
        assertThat(response.score()).isEqualTo(2);
        assertThat(response.totalQuestions()).isEqualTo(3);
        assertThat(response.isFirstAttempt()).isFalse();
        assertThat(response.attemptedAt()).isEqualTo(OffsetDateTime.parse("2026-03-16T13:00:00Z"));
    }

    @Test
    void getLatestQuizAttemptReturnsNotFoundWhenAttemptDoesNotExist() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.findFirstByLearner_IdAndQuiz_QuizIdOrderByAttemptedAtDescAttemptIdDesc(
                learnerUser.userId(),
                quiz.getQuizId()
        )).thenReturn(java.util.Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.getLatestQuizAttempt(quiz.getPublicId(), learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("No quiz attempt found");
    }

    @Test
    void getBestQuizAttemptReturnsHighestScoringAttempt() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();
        QuizAttempt bestAttempt = new QuizAttempt(
                quiz,
                learnerUser.learner(),
                (short) 3,
                false,
                OffsetDateTime.parse("2026-03-16T11:00:00Z")
        );

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.findFirstByLearner_IdAndQuiz_QuizIdOrderByScoreDescAttemptedAtDescAttemptIdDesc(
                learnerUser.userId(),
                quiz.getQuizId()
        )).thenReturn(java.util.Optional.of(bestAttempt));

        QuizAttemptResponse response = learnerQuizAttemptService.getBestQuizAttempt(quiz.getPublicId(), learnerUser);

        assertThat(response.quizPublicId()).isEqualTo(quiz.getPublicId());
        assertThat(response.score()).isEqualTo(3);
        assertThat(response.totalQuestions()).isEqualTo(3);
        assertThat(response.isFirstAttempt()).isFalse();
        assertThat(response.attemptedAt()).isEqualTo(OffsetDateTime.parse("2026-03-16T11:00:00Z"));
    }

    @Test
    void getBestQuizAttemptReturnsNotFoundWhenAttemptDoesNotExist() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.findFirstByLearner_IdAndQuiz_QuizIdOrderByScoreDescAttemptedAtDescAttemptIdDesc(
                learnerUser.userId(),
                quiz.getQuizId()
        )).thenReturn(java.util.Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.getBestQuizAttempt(quiz.getPublicId(), learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("No quiz attempt found");
    }

    @Test
    void getLatestQuizAttemptRejectsLessonCreator() {
        SupabaseAuthUser ownerUser = ownerUser();
        Quiz quiz = buildQuizWithMixedQuestions(LessonModerationStatus.APPROVED, ownerUser.userId());

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.getLatestQuizAttempt(quiz.getPublicId(), ownerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Lesson creators cannot answer their own quiz");
    }

    @Test
    void getBestQuizAttemptRejectsLessonCreator() {
        SupabaseAuthUser ownerUser = ownerUser();
        Quiz quiz = buildQuizWithMixedQuestions(LessonModerationStatus.APPROVED, ownerUser.userId());

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.getBestQuizAttempt(quiz.getPublicId(), ownerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Lesson creators cannot answer their own quiz");
    }

    @Test
    void submitQuizAttemptRejectsDuplicateQuestionAnswers() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("Duplicate answer for question");
    }

    @Test
    void submitQuizAttemptRejectsMissingQuestionsOrWrongAnswerCount() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("Submission must answer every question exactly once");
    }

    @Test
    void submitQuizAttemptRejectsInvalidOptionIds() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("not-an-option")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("Invalid option selected for question");
    }

    @Test
    void submitQuizAttemptRejectsQuestionIdsOutsideQuiz() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(UUID.randomUUID(), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("Question does not belong to quiz");
    }

    @Test
    void submitQuizAttemptRejectsQuizWhenLessonIsNotApproved() {
        Quiz quiz = buildQuizWithMixedQuestions(LessonModerationStatus.UNPUBLISHED);
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Quiz is only available for approved lessons");
    }

    @Test
    void submitQuizAttemptRejectsLessonCreator() {
        SupabaseAuthUser ownerUser = ownerUser();
        Quiz quiz = buildQuizWithMixedQuestions(LessonModerationStatus.APPROVED, ownerUser.userId());

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, ownerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Lesson creators cannot answer their own quiz");
    }

    @Test
    void contributorCanSubmitQuizAttemptForAnotherContributorsApprovedLesson() {
        SupabaseAuthUser contributorUser = contributorUser();
        Quiz quiz = buildQuizWithMixedQuestions(LessonModerationStatus.APPROVED, UUID.randomUUID());

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(contributorUser.userId(), quiz.getLesson().getPublicId())).thenReturn(true);
        when(quizAttemptRepository.existsByLearner_IdAndQuiz_QuizId(contributorUser.userId(), quiz.getQuizId())).thenReturn(false);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        QuizAttemptResponse response = learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, contributorUser);

        assertThat(response.score()).isEqualTo(3);
        assertThat(response.isFirstAttempt()).isTrue();
        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getLearner()).isEqualTo(contributorUser.learner());
    }

    @Test
    void contributorCannotSubmitQuizAttemptForOwnLesson() {
        SupabaseAuthUser contributorUser = contributorUser();
        Quiz quiz = buildQuizWithMixedQuestions(LessonModerationStatus.APPROVED, contributorUser.userId());

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, contributorUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Lesson creators cannot answer their own quiz");
    }

    @Test
    void unsupportedCallerTypeIsForbidden() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser unsupportedUser = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, unsupportedUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Learner or contributor account required");
    }

    @Test
    void submitQuizAttemptRequiresEnrollment() {
        Quiz quiz = buildQuizWithMixedQuestions();
        SupabaseAuthUser learnerUser = learnerUser();

        when(quizRepository.findByPublicId(quiz.getPublicId())).thenReturn(java.util.Optional.of(quiz));
        when(lessonEnrollmentService.isEnrolled(learnerUser.userId(), quiz.getLesson().getPublicId())).thenReturn(false);

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 0), List.of("mcq-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 1), List.of("multi-a", "multi-b")),
                new QuizQuestionAnswerRequest(questionPublicId(quiz, 2), List.of("true"))
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> learnerQuizAttemptService.submitQuizAttempt(quiz.getPublicId(), request, learnerUser)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("You must be enrolled in the lesson to access quizzes");
    }

    private Quiz buildQuizWithMixedQuestions() {
        return buildQuizWithMixedQuestions(LessonModerationStatus.APPROVED, UUID.randomUUID());
    }

    private Quiz buildQuizWithMixedQuestions(LessonModerationStatus lessonModerationStatus) {
        return buildQuizWithMixedQuestions(lessonModerationStatus, UUID.randomUUID());
    }

    private Quiz buildQuizWithMixedQuestions(LessonModerationStatus lessonModerationStatus, UUID contributorId) {
        Lesson lesson = new Lesson();
        lesson.setLessonModerationStatus(lessonModerationStatus);
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        lesson.setContributor(contributor);
        Quiz quiz = new Quiz(lesson, OffsetDateTime.parse("2026-03-16T10:00:00Z"));
        ReflectionTestUtils.setField(quiz, "quizId", 42);
        ReflectionTestUtils.setField(quiz, "publicId", UUID.randomUUID());

        MCQQuestion mcq = new MCQQuestion(
                quiz,
                "What is 2 + 2?",
                0,
                objectMapper.valueToTree(List.of(
                        java.util.Map.of("id", "mcq-a", "text", "3"),
                        java.util.Map.of("id", "mcq-b", "text", "4")
                )),
                "mcq-b"
        );
        ReflectionTestUtils.setField(mcq, "publicId", UUID.randomUUID());

        MultiSelectQuestion multi = new MultiSelectQuestion(
                quiz,
                "Select prime numbers",
                1,
                objectMapper.valueToTree(List.of(
                        java.util.Map.of("id", "multi-a", "text", "2"),
                        java.util.Map.of("id", "multi-b", "text", "3"),
                        java.util.Map.of("id", "multi-c", "text", "4")
                )),
                objectMapper.valueToTree(List.of("multi-a", "multi-b"))
        );
        ReflectionTestUtils.setField(multi, "publicId", UUID.randomUUID());

        TrueFalseQuestion trueFalse = new TrueFalseQuestion(
                quiz,
                "The sky is blue.",
                2,
                true
        );
        ReflectionTestUtils.setField(trueFalse, "publicId", UUID.randomUUID());

        quiz.setQuestions(new ArrayList<>(List.of(mcq, multi, trueFalse)));
        return quiz;
    }

    private UUID questionPublicId(Quiz quiz, int index) {
        return quiz.getQuestions().get(index).getPublicId();
    }

    private SupabaseAuthUser learnerUser() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "learner-" + learnerId,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        return new SupabaseAuthUser(learnerId, learner, null);
    }

    private SupabaseAuthUser ownerUser() {
        UUID userId = UUID.randomUUID();
        Learner learner = new Learner(
                userId,
                UUID.randomUUID(),
                "owner-" + userId,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        Contributor contributor = new Contributor();
        contributor.setContributorId(userId);
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.parse("2026-03-02T00:00:00Z"));
        return new SupabaseAuthUser(userId, learner, contributor);
    }

    private SupabaseAuthUser contributorUser() {
        UUID userId = UUID.randomUUID();
        Learner learner = new Learner(
                userId,
                UUID.randomUUID(),
                "contributor-" + userId,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        Contributor contributor = new Contributor();
        contributor.setContributorId(userId);
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.parse("2026-03-02T00:00:00Z"));
        return new SupabaseAuthUser(userId, learner, contributor);
    }
}
