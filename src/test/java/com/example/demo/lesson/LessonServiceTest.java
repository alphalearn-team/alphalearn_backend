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
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

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
}
