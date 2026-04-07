package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.concept.Concept;
import com.example.demo.lesson.enrollment.LessonEnrollmentService;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.contributor.Contributor;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.lesson.dto.CreateLessonRequest;
import com.example.demo.lesson.dto.UpdateLessonRequest;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.dto.LessonContributorSummaryDto;
import com.example.demo.lesson.dto.LessonConceptSummaryDto;
import com.example.demo.lesson.dto.LessonDetailDto;
import com.example.demo.lesson.dto.LessonDetailView;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lesson.dto.LessonPublicDetailDto;
import com.example.demo.lesson.dto.LessonPublicSummaryDto;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.example.demo.lesson.query.LessonListAudience;
import com.example.demo.lesson.query.LessonListCriteria;
import com.example.demo.lesson.query.LessonListQueryService;
import com.example.demo.lesson.read.LessonLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ContributorRepository contributorRepository;
    private final LessonLookupService lessonLookupService;
    private final LessonModerationWorkflowService lessonModerationWorkflowService;
    private final LessonListQueryService lessonListQueryService;
    private final LessonSectionService lessonSectionService;
    private final LessonEnrollmentService lessonEnrollmentService;
    private final ObjectMapper objectMapper;
    private final LessonServiceValidationSupport validationSupport;
    private final LessonConceptSupport conceptSupport;
    private final LessonDetailAssembler detailAssembler;

    public LessonService(
            LessonRepository lessonRepository,
            ContributorRepository contributorRepository,
            ConceptRepository conceptRepository,
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService,
            LessonMappingSupport lessonMappingSupport,
            LessonListQueryService lessonListQueryService,
            LessonModerationRecordRepository lessonModerationRecordRepository,
            LessonSectionService lessonSectionService,
            LessonEnrollmentService lessonEnrollmentService,
            ObjectMapper objectMapper) {
        this.lessonRepository = lessonRepository;
        this.contributorRepository = contributorRepository;
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
        this.lessonListQueryService = lessonListQueryService;
        this.lessonSectionService = lessonSectionService;
        this.lessonEnrollmentService = lessonEnrollmentService;
        this.objectMapper = objectMapper;
        this.validationSupport = new LessonServiceValidationSupport();
        this.conceptSupport = new LessonConceptSupport(conceptRepository);
        this.detailAssembler = new LessonDetailAssembler(
                lessonMappingSupport,
                lessonModerationRecordRepository,
                lessonSectionService,
                lessonEnrollmentService,
                objectMapper
        );
    }

    @Transactional(readOnly = true)
    public List<LessonContributorSummaryDto> getMyAuthoredLessons(
            UUID ownerUserId,
            List<UUID> conceptPublicIds) {
        List<Integer> conceptIds = resolveConceptIdsByPublicIds(conceptPublicIds);
        List<Lesson> lessons = lessonListQueryService.findLessons(new LessonListCriteria(
                conceptIds,
                ownerUserId,
                null,
                LessonListAudience.CONTRIBUTOR));

        return lessons.stream()
                .map(detailAssembler::toContributorSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonPublicSummaryDto> findPublicLessons(List<UUID> conceptPublicIds) {
        List<Integer> conceptIds = resolveConceptIdsByPublicIds(conceptPublicIds);
        List<Lesson> lessons = lessonListQueryService.findLessons(new LessonListCriteria(
                conceptIds,
                null,
                null,
                LessonListAudience.PUBLIC));
        return lessons.stream()
                .map(detailAssembler::toPublicSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonDetailView getLessonDetailForUser(UUID lessonPublicId, SupabaseAuthUser user) {
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        if (user != null && user.userId() != null
                && lesson.getContributor() != null
                && lesson.getContributor().getContributorId().equals(user.userId())
                && lesson.getDeletedAt() == null) {
            return toDetailDto(lesson);
        }

        Lesson publicLesson = lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId);
        boolean enrolled = user != null && user.userId() != null
                && lessonEnrollmentService.isEnrolled(user.userId(), lessonPublicId);
        return toPublicDetailDto(publicLesson, enrolled);
    }

    @Transactional
    public LessonDetailDto createLesson(CreateLessonRequest request, SupabaseAuthUser user) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        validationSupport.requireContributorUser(user);

        String title = validationSupport.trimToNull(request.title());
        Object content = request.content();
        List<UUID> conceptPublicIds = normalizeCreateConceptPublicIds(request.conceptPublicIds());
        UUID contributorId = user.userId();

        // Support both legacy content and new sections format
        boolean hasSections = request.sections() != null && !request.sections().isEmpty();
        boolean hasContent = content != null;

        if (title == null || conceptPublicIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title and conceptPublicIds are required");
        }

        // Require at least content OR sections
        if (!hasContent && !hasSections) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Either content or sections must be provided");
        }

        if (contributorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user id is required");
        }

        Contributor contributor = contributorRepository.findById(contributorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor not found"));
        List<Concept> concepts = conceptSupport.resolveConceptEntities(conceptPublicIds);

        boolean submit = Boolean.TRUE.equals(request.submit());
        Lesson lesson = new Lesson(
                title,
                hasContent ? objectMapper.valueToTree(content) : null,
                LessonModerationStatus.UNPUBLISHED,
                contributor,
                OffsetDateTime.now());
        lesson.getConcepts().addAll(concepts);

        Lesson saved = lessonRepository.save(lesson);

        // Create sections if provided
        if (hasSections) {
            lessonSectionService.createSectionsForLesson(saved, request.sections());
        }

        if (submit) {
            saved = lessonModerationWorkflowService.submitForReview(saved);
        }

        return toDetailDto(saved);
    }

    @Transactional
    public LessonDetailDto updateLesson(UUID lessonPublicId, UpdateLessonRequest request, SupabaseAuthUser user) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        validationSupport.requireContributorUser(user);

        String title = validationSupport.trimToNull(request.title());
        Object content = request.content();

        // Support both legacy content and new sections format
        boolean hasSections = request.sections() != null && !request.sections().isEmpty();
        boolean hasContent = content != null && !validationSupport.isEmptyContent(content);

        if (title == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title is required");
        }

        // Require at least content OR sections
        if (!hasContent && !hasSections) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Either content or sections must be provided");
        }

        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        validationSupport.requireOwner(lesson, user);
        LessonModerationStatus previousStatus = lesson.getLessonModerationStatus();

        lesson.setTitle(title);
        // Update content if provided (even empty object for backward compatibility)
        if (content != null) {
            lesson.setContent(objectMapper.valueToTree(content));
        }

        // Replace sections if provided
        if (hasSections) {
            lessonSectionService.replaceSectionsForLesson(lesson, request.sections());
        }

        Lesson saved = (previousStatus == LessonModerationStatus.APPROVED
                || previousStatus == LessonModerationStatus.PENDING)
                        ? lessonModerationWorkflowService.submitForReview(lesson)
                        : lessonRepository.save(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto submitLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        validationSupport.requireContributorUser(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        validationSupport.requireOwner(lesson, user);
        LessonModerationStatus status = lesson.getLessonModerationStatus();

        if (status != LessonModerationStatus.UNPUBLISHED && status != LessonModerationStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only UNPUBLISHED or REJECTED lessons can be submitted for review.");
        }

        Lesson saved = lessonModerationWorkflowService.submitForReview(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto unpublishLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        validationSupport.requireContributorUser(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        validationSupport.requireOwner(lesson, user);

        Lesson saved = lessonModerationWorkflowService.unpublish(lesson);
        return toDetailDto(saved);
    }

    @Transactional
    public void softDeleteLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }

        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        validationSupport.requireOwner(lesson, user);

        if (lesson.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        lesson.setDeletedAt(OffsetDateTime.now());
        lessonRepository.save(lesson);
    }

    private List<UUID> normalizeCreateConceptPublicIds(List<UUID> rawConceptPublicIds) {
        return conceptSupport.normalizeCreateConceptPublicIds(rawConceptPublicIds);
    }

    private List<Integer> resolveConceptIdsByPublicIds(List<UUID> conceptPublicIds) {
        return conceptSupport.resolveConceptIdsByPublicIds(conceptPublicIds);
    }

    private LessonPublicSummaryDto toPublicSummaryDto(Lesson lesson) {
        return detailAssembler.toPublicSummaryDto(lesson);
    }

    private LessonContributorSummaryDto toContributorSummaryDto(Lesson lesson) {
        return detailAssembler.toContributorSummaryDto(lesson);
    }

    private LessonDetailDto toDetailDto(Lesson lesson) {
        return detailAssembler.toDetailDto(lesson);
    }

    private LessonPublicDetailDto toPublicDetailDto(Lesson lesson, boolean enrolled) {
        return detailAssembler.toPublicDetailDto(lesson, enrolled);
    }
}
