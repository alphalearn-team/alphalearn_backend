package com.example.demo.admin.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonMappingSupport;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.LessonModerationWorkflowService;
import com.example.demo.lesson.LessonSection;
import com.example.demo.lesson.LessonSectionService;
import com.example.demo.lesson.SectionType;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lesson.dto.LessonSectionDto;
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.example.demo.lesson.query.LessonListQueryService;
import com.example.demo.lessonreport.LessonReportRepository;
import com.example.demo.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminLessonServiceTest {

    @Mock
    private LessonLookupService lessonLookupService;

    @Mock
    private LessonModerationWorkflowService lessonModerationWorkflowService;

    @Mock
    private LessonMappingSupport lessonMappingSupport;

    @Mock
    private LessonListQueryService lessonListQueryService;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private LessonModerationRecordRepository lessonModerationRecordRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LessonSectionService lessonSectionService;

    @Mock
    private LessonReportRepository lessonReportRepository;

    private AdminLessonService adminLessonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adminLessonService = new AdminLessonService(
                lessonLookupService,
                lessonModerationWorkflowService,
                lessonMappingSupport,
                lessonListQueryService,
                conceptRepository,
                lessonModerationRecordRepository,
                objectMapper,
                notificationService,
                lessonSectionService,
                lessonReportRepository
        );
    }

    @Test
    void getLessonByPublicIdIncludesSections() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        
        Lesson lesson = new Lesson();
        lesson.setTitle("Test Lesson");
        lesson.setContent(objectMapper.valueToTree(Map.of("body", "test content")));
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
        
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        lesson.setContributor(contributor);

        LessonSection section1 = new LessonSection(
                lesson,
                (short) 0,
                SectionType.TEXT,
                "Introduction",
                objectMapper.valueToTree(Map.of("html", "<p>Welcome</p>")),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        LessonSection section2 = new LessonSection(
                lesson,
                (short) 1,
                SectionType.DEFINITION,
                null,
                objectMapper.valueToTree(Map.of("term", "Test", "definition", "A test term")),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        List<LessonSection> sections = List.of(section1, section2);
        
        LessonSectionDto dto1 = new LessonSectionDto(
                UUID.randomUUID(),
                0,
                "text",
                "Introduction",
                Map.of("html", "<p>Welcome</p>")
        );

        LessonSectionDto dto2 = new LessonSectionDto(
                UUID.randomUUID(),
                1,
                "definition",
                null,
                Map.of("term", "Test", "definition", "A test term")
        );

        List<LessonSectionDto> sectionDtos = List.of(dto1, dto2);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceInOrderByRecordedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(lessonMappingSupport.conceptPublicIds(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.author(lesson)).thenReturn(new LessonAuthorDto(contributorId, "testuser"));
        when(lessonSectionService.getSectionsForLesson(lesson)).thenReturn(sections);
        when(lessonSectionService.toSectionDtos(sections)).thenReturn(sectionDtos);

        AdminLessonReviewDto result = adminLessonService.getLessonByPublicId(lessonPublicId);

        assertThat(result.sections()).isNotNull();
        assertThat(result.sections()).hasSize(2);
        assertThat(result.totalSections()).isEqualTo(2);
        assertThat(result.sections().get(0).sectionType()).isEqualTo("text");
        assertThat(result.sections().get(0).title()).isEqualTo("Introduction");
        assertThat(result.sections().get(1).sectionType()).isEqualTo("definition");
    }

    @Test
    void getLessonByPublicIdWithNoSectionsReturnsEmptyList() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        
        Lesson lesson = new Lesson();
        lesson.setTitle("Test Lesson Without Sections");
        lesson.setContent(objectMapper.valueToTree(Map.of("body", "legacy content")));
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
        
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        lesson.setContributor(contributor);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceInOrderByRecordedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(lessonMappingSupport.conceptPublicIds(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.author(lesson)).thenReturn(new LessonAuthorDto(contributorId, "testuser"));
        when(lessonSectionService.getSectionsForLesson(lesson)).thenReturn(List.of());
        when(lessonSectionService.toSectionDtos(List.of())).thenReturn(List.of());

        AdminLessonReviewDto result = adminLessonService.getLessonByPublicId(lessonPublicId);

        assertThat(result.sections()).isNotNull();
        assertThat(result.sections()).isEmpty();
        assertThat(result.totalSections()).isEqualTo(0);
    }

    @Test
    void getLessonByPublicIdIncludesAutomatedModerationReasons() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        
        Lesson lesson = new Lesson();
        lesson.setTitle("Test Lesson");
        lesson.setContent(objectMapper.valueToTree(Map.of("body", "test content")));
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
        
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        lesson.setContributor(contributor);

        LessonModerationRecord automatedRecord = new LessonModerationRecord();
        automatedRecord.setEventType(LessonModerationEventType.AUTO_FLAGGED);
        automatedRecord.setDecisionSource(LessonModerationDecisionSource.AUTO);
        automatedRecord.setReasons(objectMapper.valueToTree(List.of("Potential policy violation", "Requires manual review")));

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceInOrderByRecordedAtDesc(any(), any()))
                .thenReturn(Optional.of(automatedRecord));
        when(lessonModerationRecordRepository.findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(lessonMappingSupport.conceptPublicIds(lesson)).thenReturn(List.of());
        when(lessonMappingSupport.author(lesson)).thenReturn(new LessonAuthorDto(contributorId, "testuser"));
        when(lessonSectionService.getSectionsForLesson(lesson)).thenReturn(List.of());
        when(lessonSectionService.toSectionDtos(List.of())).thenReturn(List.of());

        AdminLessonReviewDto result = adminLessonService.getLessonByPublicId(lessonPublicId);

        assertThat(result.automatedModerationReasons()).hasSize(2);
        assertThat(result.automatedModerationReasons()).contains("Potential policy violation", "Requires manual review");
    }

    @Test
    void updateLessonModerationStatusUnpublishesAndResolvesPendingReportsByDefault() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID contributorLearnerId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        lesson.setPublicId(lessonPublicId);
        lesson.setTitle("Test Lesson");
        lesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
        Learner contributorLearner = new Learner();
        contributorLearner.setId(contributorLearnerId);
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorLearnerId);
        contributor.setLearner(contributorLearner);
        lesson.setContributor(contributor);
        org.springframework.test.util.ReflectionTestUtils.setField(lesson, "lessonId", 77);

        Lesson unpublished = new Lesson();
        unpublished.setPublicId(lessonPublicId);
        unpublished.setTitle("Test Lesson");
        unpublished.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        unpublished.setContributor(contributor);
        org.springframework.test.util.ReflectionTestUtils.setField(unpublished, "lessonId", 77);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationWorkflowService.unpublish(lesson)).thenReturn(unpublished);

        AdminLessonDetailDto result = adminLessonService.updateLessonModerationStatus(
                lessonPublicId,
                new AdminUpdateLessonModerationStatusRequest(LessonModerationStatus.UNPUBLISHED, null),
                new SupabaseAuthUser(adminUserId, null, null)
        );

        assertThat(result.lessonModerationStatus()).isEqualTo(LessonModerationStatus.UNPUBLISHED);
        org.mockito.Mockito.verify(lessonReportRepository).resolvePendingForLessonId(
                org.mockito.ArgumentMatchers.eq(77),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(adminUserId),
                org.mockito.ArgumentMatchers.any()
        );
        verify(notificationService).create(
                org.mockito.ArgumentMatchers.eq(contributorLearnerId),
                org.mockito.ArgumentMatchers.contains("has been unpublished by admin")
        );
    }

    @Test
    void updateLessonModerationStatusRejectsUnsupportedStatus() {
        UUID lessonPublicId = UUID.randomUUID();

        org.springframework.web.server.ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> adminLessonService.updateLessonModerationStatus(
                        lessonPublicId,
                        new AdminUpdateLessonModerationStatusRequest(LessonModerationStatus.APPROVED, true),
                        new SupabaseAuthUser(UUID.randomUUID(), null, null)
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("Only UNPUBLISHED is supported by this endpoint");
    }
}
