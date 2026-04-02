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

class LessonEnrollmentServiceTest {

    private LessonEnrollmentRepository repository;
    private LessonLookupService lessonLookupService;
    private LessonEnrollmentService service;
    private LearnerRepository learnerRepository;

    @BeforeEach
    void setUp() {
        repository = mock(LessonEnrollmentRepository.class);
        lessonLookupService = mock(LessonLookupService.class);
        learnerRepository = mock(LearnerRepository.class);
        service = new LessonEnrollmentService(repository, lessonLookupService,learnerRepository);
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

        LessonEnrollmentStatusDto status = service.getEnrollmentStatus(lessonId, user);
        assertThat(status.enrolled()).isTrue();
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
        
        Contributor contributor = new Contributor();
        contributor.setContributorId(creatorId);
        lesson.setContributor(contributor);
        
        return lesson;
    }
}
