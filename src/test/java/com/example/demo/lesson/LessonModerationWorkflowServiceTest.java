package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.contributor.Contributor;
import com.example.demo.lesson.moderation.LessonAutoModerationService;
import com.example.demo.lesson.moderation.LessonModerationDecision;
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.example.demo.lesson.moderation.LessonModerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonModerationWorkflowServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonModerationRecordRepository lessonModerationRecordRepository;

    @Mock
    private LessonAutoModerationService lessonAutoModerationService;

    private LessonModerationWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new LessonModerationWorkflowService(
                lessonRepository,
                lessonModerationRecordRepository,
                lessonAutoModerationService,
                new ObjectMapper()
        );
    }

    @Test
    void submitForReviewRoutesAutoApprovedLessonToPendingAndWritesAutoApprovedRecord() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.UNPUBLISHED);
        when(lessonAutoModerationService.moderate(lesson)).thenReturn(new LessonModerationResult(
                LessonModerationDecision.APPROVE,
                List.of("Looks good"),
                "TEST_PROVIDER",
                java.util.Map.of("decision", "APPROVE"),
                OffsetDateTime.parse("2026-03-03T10:15:30Z")
        ));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        Lesson saved = service.submitForReview(lesson);

        assertThat(saved.getLessonModerationStatus()).isEqualTo(LessonModerationStatus.PENDING);
        ArgumentCaptor<LessonModerationRecord> captor = ArgumentCaptor.forClass(LessonModerationRecord.class);
        verify(lessonModerationRecordRepository).save(captor.capture());
        LessonModerationRecord record = captor.getValue();
        assertThat(record.getEventType()).isEqualTo(LessonModerationEventType.AUTO_APPROVED);
        assertThat(record.getDecisionSource()).isEqualTo(LessonModerationDecisionSource.AUTO);
        assertThat(record.getResultingStatus()).isEqualTo(LessonModerationStatus.PENDING);
        assertThat(record.getProviderName()).isEqualTo("TEST_PROVIDER");
        assertThat(record.getRecordedAt()).isEqualTo(OffsetDateTime.parse("2026-03-03T10:15:30Z"));
    }

    @Test
    void submitForReviewRejectsLessonAndWritesAutoRejectedRecord() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.UNPUBLISHED);
        when(lessonAutoModerationService.moderate(lesson)).thenReturn(new LessonModerationResult(
                LessonModerationDecision.REJECT,
                List.of("Rejected keyword detected: hate"),
                "TEST_PROVIDER",
                null,
                OffsetDateTime.now()
        ));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        service.submitForReview(lesson);

        assertThat(lesson.getLessonModerationStatus()).isEqualTo(LessonModerationStatus.REJECTED);
        ArgumentCaptor<LessonModerationRecord> captor = ArgumentCaptor.forClass(LessonModerationRecord.class);
        verify(lessonModerationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(LessonModerationEventType.AUTO_REJECTED);
    }

    @Test
    void submitForReviewFlagsLessonAndLeavesItPending() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.UNPUBLISHED);
        when(lessonAutoModerationService.moderate(lesson)).thenReturn(new LessonModerationResult(
                LessonModerationDecision.FLAG,
                List.of("Flagged keyword detected: unsafe"),
                "TEST_PROVIDER",
                null,
                OffsetDateTime.now()
        ));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        service.submitForReview(lesson);

        assertThat(lesson.getLessonModerationStatus()).isEqualTo(LessonModerationStatus.PENDING);
        ArgumentCaptor<LessonModerationRecord> captor = ArgumentCaptor.forClass(LessonModerationRecord.class);
        verify(lessonModerationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(LessonModerationEventType.AUTO_FLAGGED);
    }

    @Test
    void submitForReviewFallsBackToPendingWhenModerationFails() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.UNPUBLISHED);
        when(lessonAutoModerationService.moderate(lesson)).thenThrow(new IllegalStateException("provider timeout"));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        service.submitForReview(lesson);

        assertThat(lesson.getLessonModerationStatus()).isEqualTo(LessonModerationStatus.PENDING);
        ArgumentCaptor<LessonModerationRecord> captor = ArgumentCaptor.forClass(LessonModerationRecord.class);
        verify(lessonModerationRecordRepository).save(captor.capture());
        LessonModerationRecord record = captor.getValue();
        assertThat(record.getEventType()).isEqualTo(LessonModerationEventType.AUTO_FAILED);
        assertThat(record.getDecisionSource()).isEqualTo(LessonModerationDecisionSource.AUTO_FALLBACK);
        assertThat(record.getFailureMessage()).isEqualTo("provider timeout");
    }

    @Test
    void approveWritesAdminApprovedRecord() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.PENDING);
        UUID actorUserId = UUID.randomUUID();
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        Lesson saved = service.approve(lesson, actorUserId);

        assertThat(saved.getLessonModerationStatus()).isEqualTo(LessonModerationStatus.APPROVED);
        ArgumentCaptor<LessonModerationRecord> captor = ArgumentCaptor.forClass(LessonModerationRecord.class);
        verify(lessonModerationRecordRepository).save(captor.capture());
        LessonModerationRecord record = captor.getValue();
        assertThat(record.getEventType()).isEqualTo(LessonModerationEventType.ADMIN_APPROVED);
        assertThat(record.getDecisionSource()).isEqualTo(LessonModerationDecisionSource.ADMIN);
        assertThat(record.getActorUserId()).isEqualTo(actorUserId);
        assertThat(record.getReviewNote()).isNull();
    }

    @Test
    void rejectWritesAdminRejectedRecord() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.PENDING);
        UUID actorUserId = UUID.randomUUID();
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        Lesson saved = service.reject(lesson, "Needs revision", actorUserId);

        assertThat(saved.getLessonModerationStatus()).isEqualTo(LessonModerationStatus.REJECTED);
        ArgumentCaptor<LessonModerationRecord> captor = ArgumentCaptor.forClass(LessonModerationRecord.class);
        verify(lessonModerationRecordRepository).save(captor.capture());
        LessonModerationRecord record = captor.getValue();
        assertThat(record.getEventType()).isEqualTo(LessonModerationEventType.ADMIN_REJECTED);
        assertThat(record.getDecisionSource()).isEqualTo(LessonModerationDecisionSource.ADMIN);
        assertThat(record.getActorUserId()).isEqualTo(actorUserId);
        assertThat(record.getReviewNote()).isEqualTo("Needs revision");
        assertThat(record.getResultingStatus()).isEqualTo(LessonModerationStatus.REJECTED);
    }

    @Test
    void approveRejectStillRequirePendingStatus() {
        Lesson lesson = lessonWithStatus(LessonModerationStatus.UNPUBLISHED);

        assertThrows(ResponseStatusException.class, () -> service.approve(lesson, UUID.randomUUID()));
        assertThrows(ResponseStatusException.class, () -> service.reject(lesson, "Needs revision", UUID.randomUUID()));
    }

    private Lesson lessonWithStatus(LessonModerationStatus status) {
        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson title");
        lesson.setLessonModerationStatus(status);
        lesson.setCreatedAt(OffsetDateTime.now());

        Contributor contributor = new Contributor();
        contributor.setContributorId(UUID.randomUUID());
        contributor.setPromotedAt(OffsetDateTime.now());
        lesson.setContributor(contributor);
        return lesson;
    }
}
