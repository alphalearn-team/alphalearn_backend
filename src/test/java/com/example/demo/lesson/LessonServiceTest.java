package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.dto.CreateLessonRequest;
import com.example.demo.lesson.dto.LessonDetailDto;
import com.example.demo.lesson.dto.LessonEnrolledDetailDTO;
import com.example.demo.lesson.dto.UpdateLessonRequest;
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.example.demo.lessonenrollment.LessonEnrollmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonEnrollmentRepository lessonEnrollmentRepository;

    @Mock
    private ContributorRepository contributorRepository;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private LessonLookupService lessonLookupService;

    @Mock
    private LessonModerationWorkflowService lessonModerationWorkflowService;

    @Mock
    private LessonMappingSupport lessonMappingSupport;

    @Mock
    private com.example.demo.lesson.query.LessonListQueryService lessonListQueryService;

    @Mock
    private LessonModerationRecordRepository lessonModerationRecordRepository;

    private LessonService lessonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(
                lessonEnrollmentRepository,
                lessonRepository,
                contributorRepository,
                conceptRepository,
                lessonLookupService,
                lessonModerationWorkflowService,
                lessonMappingSupport,
                lessonListQueryService,
                lessonModerationRecordRepository,
                objectMapper
        );
    }

    @Test
    void createLessonWithSubmitFalseCreatesDraftWithoutModeration() {
        UUID contributorId = UUID.randomUUID();
        UUID conceptPublicId = UUID.randomUUID();
        SupabaseAuthUser user = contributorUser(contributorId);
        Contributor contributor = contributor(contributorId);
        Concept concept = concept(conceptPublicId, 1);

        when(contributorRepository.findById(contributorId)).thenReturn(Optional.of(contributor));
        when(conceptRepository.findAllByPublicIdIn(List.of(conceptPublicId))).thenReturn(List.of(concept));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonModerationRecordRepository.findTopByLessonOrderByRecordedAtDesc(any())).thenReturn(Optional.empty());

        LessonDetailDto result = lessonService.createLesson(
                new CreateLessonRequest("Draft lesson", java.util.Map.of("body", "hello"), List.of(conceptPublicId), false),
                user
        );

        assertThat(result.moderationStatus()).isEqualTo("UNPUBLISHED");
        verify(lessonModerationWorkflowService, never()).submitForReview(any());
    }

    @Test
    void createLessonWithSubmitTrueReusesModerationWorkflow() {
        UUID contributorId = UUID.randomUUID();
        UUID conceptPublicId = UUID.randomUUID();
        SupabaseAuthUser user = contributorUser(contributorId);
        Contributor contributor = contributor(contributorId);
        Concept concept = concept(conceptPublicId, 1);

        when(contributorRepository.findById(contributorId)).thenReturn(Optional.of(contributor));
        when(conceptRepository.findAllByPublicIdIn(List.of(conceptPublicId))).thenReturn(List.of(concept));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonModerationWorkflowService.submitForReview(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
            return lesson;
        });
        when(lessonModerationRecordRepository.findTopByLessonOrderByRecordedAtDesc(any())).thenReturn(Optional.empty());

        LessonDetailDto result = lessonService.createLesson(
                new CreateLessonRequest("Submitted lesson", java.util.Map.of("body", "hello"), List.of(conceptPublicId), true),
                user
        );

        assertThat(result.moderationStatus()).isEqualTo("APPROVED");
        verify(lessonModerationWorkflowService).submitForReview(any(Lesson.class));
    }

    @Test
    void submitLessonRequiresOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        Contributor contributor = contributor(ownerId);
        lesson.setContributor(contributor);
        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);

        when(lessonLookupService.findByPublicIdOrThrow(any())).thenReturn(lesson);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonService.submitLesson(UUID.randomUUID(), contributorUser(otherUserId))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void submitLessonAllowsUnpublished() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.UNPUBLISHED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationWorkflowService.submitForReview(lesson)).thenAnswer(invocation -> {
            Lesson submittedLesson = invocation.getArgument(0);
            submittedLesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
            return submittedLesson;
        });
        stubDetailMappings(lesson);

        LessonDetailDto result = lessonService.submitLesson(lessonPublicId, contributorUser(ownerId));

        assertThat(result.moderationStatus()).isEqualTo("PENDING");
        verify(lessonModerationWorkflowService).submitForReview(lesson);
    }

    @Test
    void submitLessonAllowsRejected() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.REJECTED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationWorkflowService.submitForReview(lesson)).thenAnswer(invocation -> {
            Lesson submittedLesson = invocation.getArgument(0);
            submittedLesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
            return submittedLesson;
        });
        stubDetailMappings(lesson);

        LessonDetailDto result = lessonService.submitLesson(lessonPublicId, contributorUser(ownerId));

        assertThat(result.moderationStatus()).isEqualTo("APPROVED");
        verify(lessonModerationWorkflowService).submitForReview(lesson);
    }

    @Test
    void submitLessonRejectsPendingWithConflict() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.PENDING);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonService.submitLesson(lessonPublicId, contributorUser(ownerId))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        assertThat(ex.getReason()).isEqualTo("Only UNPUBLISHED or REJECTED lessons can be submitted for review.");
        verify(lessonModerationWorkflowService, never()).submitForReview(any(Lesson.class));
    }

    @Test
    void submitLessonRejectsApprovedWithConflict() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.APPROVED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonService.submitLesson(lessonPublicId, contributorUser(ownerId))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        assertThat(ex.getReason()).isEqualTo("Only UNPUBLISHED or REJECTED lessons can be submitted for review.");
        verify(lessonModerationWorkflowService, never()).submitForReview(any(Lesson.class));
    }

    @Test
    void updateLessonUnpublishedSavesDirectlyWithoutModeration() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.UNPUBLISHED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonRepository.save(lesson)).thenAnswer(invocation -> invocation.getArgument(0));
        stubDetailMappings(lesson);

        LessonDetailDto result = lessonService.updateLesson(
                lessonPublicId,
                new UpdateLessonRequest("  Updated draft title  ", java.util.Map.of("body", "draft body")),
                contributorUser(ownerId)
        );

        assertThat(lesson.getTitle()).isEqualTo("Updated draft title");
        assertThat(result.moderationStatus()).isEqualTo("UNPUBLISHED");
        verify(lessonModerationWorkflowService, never()).submitForReview(any(Lesson.class));
    }

    @Test
    void updateLessonRejectedSavesDirectlyWithoutModeration() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.REJECTED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonRepository.save(lesson)).thenAnswer(invocation -> invocation.getArgument(0));
        stubDetailMappings(lesson);

        LessonDetailDto result = lessonService.updateLesson(
                lessonPublicId,
                new UpdateLessonRequest("Updated rejected title", java.util.Map.of("body", "rejected body")),
                contributorUser(ownerId)
        );

        assertThat(lesson.getTitle()).isEqualTo("Updated rejected title");
        assertThat(result.moderationStatus()).isEqualTo("REJECTED");
        verify(lessonModerationWorkflowService, never()).submitForReview(any(Lesson.class));
    }

    @Test
    void updateLessonPendingSavesDirectlyWithoutModeration() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.PENDING);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonRepository.save(lesson)).thenAnswer(invocation -> invocation.getArgument(0));
        stubDetailMappings(lesson);

        LessonDetailDto result = lessonService.updateLesson(
                lessonPublicId,
                new UpdateLessonRequest("Updated pending title", java.util.Map.of("body", "pending body")),
                contributorUser(ownerId)
        );

        assertThat(lesson.getTitle()).isEqualTo("Updated pending title");
        assertThat(result.moderationStatus()).isEqualTo("PENDING");
        verify(lessonModerationWorkflowService, never()).submitForReview(any(Lesson.class));
    }

    @Test
    void updateLessonApprovedReusesModerationWorkflow() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.APPROVED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationWorkflowService.submitForReview(lesson)).thenAnswer(invocation -> {
            Lesson submittedLesson = invocation.getArgument(0);
            assertThat(submittedLesson.getTitle()).isEqualTo("Approved lesson update");
            assertThat(submittedLesson.getContent()).isEqualTo(objectMapper.valueToTree(java.util.Map.of("body", "approved body")));
            submittedLesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
            return submittedLesson;
        });
        stubDetailMappings(lesson);

        LessonDetailDto result = lessonService.updateLesson(
                lessonPublicId,
                new UpdateLessonRequest("Approved lesson update", java.util.Map.of("body", "approved body")),
                contributorUser(ownerId)
        );

        assertThat(result.moderationStatus()).isEqualTo("PENDING");
        verify(lessonModerationWorkflowService).submitForReview(lesson);
        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void ownerLessonDetailIncludesLatestAdminRejectionReason() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        SupabaseAuthUser user = contributorUser(ownerId);
        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson");
        lesson.setContent(objectMapper.valueToTree(java.util.Map.of("body", "hello")));
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setLessonModerationStatus(LessonModerationStatus.REJECTED);
        lesson.setContributor(contributor(ownerId));

        LessonModerationRecord latestRecord = new LessonModerationRecord();
        latestRecord.setEventType(LessonModerationEventType.ADMIN_REJECTED);
        latestRecord.setDecisionSource(LessonModerationDecisionSource.ADMIN);
        latestRecord.setRecordedAt(OffsetDateTime.now());
        latestRecord.setReviewNote("Needs revision before publication");
        latestRecord.setReasons(objectMapper.valueToTree(List.of()));

        LessonModerationRecord latestAdminRecord = new LessonModerationRecord();
        latestAdminRecord.setEventType(LessonModerationEventType.ADMIN_REJECTED);
        latestAdminRecord.setDecisionSource(LessonModerationDecisionSource.ADMIN);
        latestAdminRecord.setRecordedAt(OffsetDateTime.now());
        latestAdminRecord.setReviewNote("Needs revision before publication");
        latestAdminRecord.setReasons(objectMapper.valueToTree(List.of()));

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationRecordRepository.findTopByLessonOrderByRecordedAtDesc(lesson)).thenReturn(Optional.of(latestRecord));
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(
                lesson,
                LessonModerationDecisionSource.ADMIN
        )).thenReturn(Optional.of(latestAdminRecord));
        when(lessonMappingSupport.conceptPublicIds(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.conceptSummaries(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.author(lesson)).thenReturn(null);

        LessonDetailDto result = (LessonDetailDto) lessonService.getLessonDetailForUser(lessonPublicId, user);

        assertThat(result.adminRejectionReason()).isEqualTo("Needs revision before publication");
    }

    @Test
    void getLessonContentForLearnerAllowsOwnerWithoutEnrollment() {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(ownerId, LessonModerationStatus.APPROVED);
        lesson.setTitle("Owner visible lesson");
        lesson.setContent(objectMapper.valueToTree(java.util.Map.of("body", "owner content")));

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonMappingSupport.conceptPublicIds(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.conceptSummaries(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.author(lesson)).thenReturn(null);

        LessonEnrolledDetailDTO result = lessonService.getLessonContentForLearner(
                lessonPublicId,
                contributorUser(ownerId)
        );

        assertThat(result.title()).isEqualTo("Owner visible lesson");
        verify(lessonEnrollmentRepository, never()).existsByLearner_IdAndLesson_LessonId(any(), any());
    }

    @Test
    void getLessonContentForLearnerRejectsWhenNotEnrolled() {
        UUID learnerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lessonForUpdate(UUID.randomUUID(), LessonModerationStatus.APPROVED);

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonEnrollmentRepository.existsByLearner_IdAndLesson_LessonId(learnerId, null)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonService.getLessonContentForLearner(lessonPublicId, contributorUser(learnerId))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
        assertThat(ex.getReason()).isEqualTo("Not enrolled in this lesson");
    }

    private SupabaseAuthUser contributorUser(UUID contributorId) {
        Learner learner = new Learner(contributorId, UUID.randomUUID(), "user-" + contributorId, OffsetDateTime.now(), (short) 0);
        Contributor contributor = contributor(contributorId);
        contributor.setLearner(learner);
        return new SupabaseAuthUser(contributorId, learner, contributor);
    }

    private Contributor contributor(UUID contributorId) {
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        contributor.setPromotedAt(OffsetDateTime.now());
        return contributor;
    }

    private Concept concept(UUID publicId, int conceptId) {
        return new Concept(conceptId, publicId, "Concept", "Desc", OffsetDateTime.now());
    }

    private Lesson lessonForUpdate(UUID ownerId, LessonModerationStatus status) {
        Lesson lesson = new Lesson();
        lesson.setTitle("Original lesson");
        lesson.setContent(objectMapper.valueToTree(java.util.Map.of("body", "original body")));
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setLessonModerationStatus(status);
        lesson.setContributor(contributor(ownerId));
        return lesson;
    }

    private void stubDetailMappings(Lesson lesson) {
        when(lessonModerationRecordRepository.findTopByLessonOrderByRecordedAtDesc(lesson)).thenReturn(Optional.empty());
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(
                lesson,
                LessonModerationDecisionSource.ADMIN
        )).thenReturn(Optional.empty());
        when(lessonMappingSupport.conceptPublicIds(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.conceptSummaries(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.author(lesson)).thenReturn(null);
    }
}
