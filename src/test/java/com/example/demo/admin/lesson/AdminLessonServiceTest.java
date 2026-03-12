package com.example.demo.admin.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.example.demo.contributor.Contributor;
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
                lessonSectionService
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
}
