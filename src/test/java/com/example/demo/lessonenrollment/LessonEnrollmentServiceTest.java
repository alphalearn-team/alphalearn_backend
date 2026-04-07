package com.example.demo.lessonenrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lessonenrollment.dto.LessonEnrollmentStatusDto;
import com.example.demo.lessonenrollment.dto.LessonEnrollmentSummaryDto;
import com.example.demo.quiz.QuizAttemptRepository;
import com.example.demo.quiz.QuizRepository;

class LessonEnrollmentServiceTest {

    private LessonEnrollmentRepository repository;
    private LessonLookupService lessonLookupService;
    private LessonEnrollmentService service;
    private LearnerRepository learnerRepository;
    private QuizAttemptRepository quizAttemptRepository;
    private QuizRepository quizRepository;

    @BeforeEach
    void setUp() {
        repository = mock(LessonEnrollmentRepository.class);
        lessonLookupService = mock(LessonLookupService.class);
        learnerRepository = mock(LearnerRepository.class);
        quizAttemptRepository = mock(QuizAttemptRepository.class);
        quizRepository = mock(QuizRepository.class);
        service = new LessonEnrollmentService(
                repository,
                lessonLookupService,
                learnerRepository,
                quizAttemptRepository,
                quizRepository
        );
    }

    @Test
    void enrollSucceedsForValidLearnerAndLesson() {
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(UUID.randomUUID());
        Lesson lesson = mockLesson(lessonId, UUID.randomUUID(), LessonModerationStatus.APPROVED);

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonId)).thenReturn(lesson);
        when(repository.existsByLearner_IdAndLesson_PublicId(user.userId(), lessonId)).thenReturn(false);

        LessonEnrollmentStatusDto result = service.enroll(lessonId, user);

        assertThat(result.enrolled()).isTrue();
        verify(repository).save(any(LessonEnrollment.class));
    }

    @Test
    void enrollThrowsForbiddenIfLessonNotApproved() {
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(UUID.randomUUID());
        Lesson lesson = mockLesson(lessonId, UUID.randomUUID(), LessonModerationStatus.UNPUBLISHED);

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonId)).thenReturn(lesson);

        assertThatThrownBy(() -> service.enroll(lessonId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        
        verify(repository, never()).save(any());
    }

    @Test
    void enrollThrowsForbiddenIfUserIsCreator() {
        UUID creatorId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(creatorId);
        Lesson lesson = mockLesson(lessonId, creatorId, LessonModerationStatus.APPROVED);

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonId)).thenReturn(lesson);

        assertThatThrownBy(() -> service.enroll(lessonId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void enrollThrowsConflictIfAlreadyEnrolled() {
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(UUID.randomUUID());
        Lesson lesson = mockLesson(lessonId, UUID.randomUUID(), LessonModerationStatus.APPROVED);

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonId)).thenReturn(lesson);
        when(repository.existsByLearner_IdAndLesson_PublicId(user.userId(), lessonId)).thenReturn(true);

        assertThatThrownBy(() -> service.enroll(lessonId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void getEnrollmentStatusReturnsTrueIfEnrolled() {
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(UUID.randomUUID());
        when(repository.existsByLearner_IdAndLesson_PublicId(user.userId(), lessonId)).thenReturn(true);
        when(lessonLookupService.findByPublicIdOrThrow(lessonId))
                .thenReturn(mockLesson(lessonId, UUID.randomUUID(), LessonModerationStatus.APPROVED));

        LessonEnrollmentStatusDto status = service.getEnrollmentStatus(lessonId, user);
        assertThat(status.enrolled()).isTrue();
    }

    @Test
    void getEnrollmentStatusReturnsFalseWhenLessonIsUnpublished() {
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(UUID.randomUUID());
        when(repository.existsByLearner_IdAndLesson_PublicId(user.userId(), lessonId)).thenReturn(true);
        when(lessonLookupService.findByPublicIdOrThrow(lessonId))
                .thenReturn(mockLesson(lessonId, UUID.randomUUID(), LessonModerationStatus.UNPUBLISHED));

        LessonEnrollmentStatusDto status = service.getEnrollmentStatus(lessonId, user);
        assertThat(status.enrolled()).isFalse();
    }

    @Test
    void getMyEnrollmentsHidesUnpublishedLessons() {
        UUID learnerId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(learnerId);
        LessonEnrollment approvedEnrollment = new LessonEnrollment();
        approvedEnrollment.setLesson(mockLesson(UUID.randomUUID(), UUID.randomUUID(), LessonModerationStatus.APPROVED));
        approvedEnrollment.setCompleted(false);
        approvedEnrollment.setFirstCompletedAt(null);

        LessonEnrollment unpublishedEnrollment = new LessonEnrollment();
        unpublishedEnrollment.setLesson(mockLesson(UUID.randomUUID(), UUID.randomUUID(), LessonModerationStatus.UNPUBLISHED));
        unpublishedEnrollment.setCompleted(false);
        unpublishedEnrollment.setFirstCompletedAt(null);

        when(repository.findByLearner_Id(learnerId)).thenReturn(List.of(approvedEnrollment, unpublishedEnrollment));

        List<LessonEnrollmentSummaryDto> result = service.getMyEnrollments(user);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).lessonPublicId()).isEqualTo(approvedEnrollment.getLesson().getPublicId());
    }

    @Test
    void getMyProgressHidesUnpublishedLessons() {
        UUID learnerId = UUID.randomUUID();
        SupabaseAuthUser user = mockUser(learnerId);
        LessonEnrollment approvedEnrollment = new LessonEnrollment();
        approvedEnrollment.setLesson(mockLesson(UUID.randomUUID(), UUID.randomUUID(), LessonModerationStatus.APPROVED));
        approvedEnrollment.setCompleted(false);

        LessonEnrollment unpublishedEnrollment = new LessonEnrollment();
        unpublishedEnrollment.setLesson(mockLesson(UUID.randomUUID(), UUID.randomUUID(), LessonModerationStatus.UNPUBLISHED));
        unpublishedEnrollment.setCompleted(false);

        when(repository.findByLearner_Id(learnerId)).thenReturn(List.of(approvedEnrollment, unpublishedEnrollment));
        when(quizRepository.countByLesson_PublicId(approvedEnrollment.getLesson().getPublicId())).thenReturn(2L);
        when(quizAttemptRepository.countFullMarkQuizzesByLearnerAndLesson(learnerId, approvedEnrollment.getLesson().getPublicId()))
                .thenReturn(1L);

        List<com.example.demo.lessonenrollment.dto.LessonProgressDto> result = service.getMyProgress(user);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).lessonPublicId()).isEqualTo(approvedEnrollment.getLesson().getPublicId());
    }

    @Test
    void countEnrollmentsDelegatesToRepository() {
        UUID lessonPublicId = UUID.randomUUID();
        when(repository.countByLesson_PublicId(lessonPublicId)).thenReturn(7L);

        long result = service.countEnrollments(lessonPublicId);

        assertThat(result).isEqualTo(7L);
        verify(repository).countByLesson_PublicId(lessonPublicId);
    }

    @Test
    void countCompletionsDelegatesToRepository() {
        UUID lessonPublicId = UUID.randomUUID();
        when(repository.countByLesson_PublicIdAndCompletedTrue(lessonPublicId)).thenReturn(3L);

        long result = service.countCompletions(lessonPublicId);

        assertThat(result).isEqualTo(3L);
        verify(repository).countByLesson_PublicIdAndCompletedTrue(lessonPublicId);
    }

    private SupabaseAuthUser mockUser(UUID userId) {
        Learner learner = new Learner();
        learner.setId(userId);
        return new SupabaseAuthUser(userId, learner, null);
    }

    private Lesson mockLesson(UUID publicId, UUID creatorId, LessonModerationStatus status) {
        Lesson lesson = new Lesson();
        lesson.setPublicId(publicId);
        lesson.setLessonModerationStatus(status);
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setDeletedAt(null);
        
        Contributor contributor = new Contributor();
        contributor.setContributorId(creatorId);
        lesson.setContributor(contributor);
        
        return lesson;
    }
}
