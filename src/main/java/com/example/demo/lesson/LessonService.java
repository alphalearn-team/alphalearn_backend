package com.example.demo.lesson;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.concept.Concept;
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
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.example.demo.lesson.query.ConceptsMatchMode;
import com.example.demo.lesson.query.LessonListAudience;
import com.example.demo.lesson.query.LessonListCriteria;
import com.example.demo.lesson.query.LessonListQueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ContributorRepository contributorRepository;
    private final ConceptRepository conceptRepository;
    private final LessonLookupService lessonLookupService;
    private final LessonModerationWorkflowService lessonModerationWorkflowService;
    private final LessonMappingSupport lessonMappingSupport;
    private final LessonListQueryService lessonListQueryService;
    private final LessonModerationRecordRepository lessonModerationRecordRepository;
    private final ObjectMapper objectMapper;
    public LessonService(
            LessonRepository lessonRepository,
            ContributorRepository contributorRepository,
            ConceptRepository conceptRepository,
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService,
            LessonMappingSupport lessonMappingSupport,
            LessonListQueryService lessonListQueryService,
            LessonModerationRecordRepository lessonModerationRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.lessonRepository = lessonRepository;
        this.contributorRepository = contributorRepository;
        this.conceptRepository = conceptRepository;
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
        this.lessonMappingSupport = lessonMappingSupport;
        this.lessonListQueryService = lessonListQueryService;
        this.lessonModerationRecordRepository = lessonModerationRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LessonContributorSummaryDto> getMyAuthoredLessons(
            UUID ownerUserId,
            List<UUID> conceptPublicIds,
            ConceptsMatchMode conceptsMatch
    ) {
        List<Integer> conceptIds = resolveConceptIdsByPublicIds(conceptPublicIds);
        List<Lesson> lessons = lessonListQueryService.findLessons(new LessonListCriteria(
                conceptIds,
                conceptsMatch,
                ownerUserId,
                null,
                LessonListAudience.CONTRIBUTOR
        ));

        return lessons.stream()
                .map(this::toContributorSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonPublicSummaryDto> findPublicLessons(List<UUID> conceptPublicIds, ConceptsMatchMode conceptsMatch) {
        List<Integer> conceptIds = resolveConceptIdsByPublicIds(conceptPublicIds);
        List<Lesson> lessons = lessonListQueryService.findLessons(new LessonListCriteria(
                conceptIds,
                conceptsMatch,
                null,
                null,
                LessonListAudience.PUBLIC
        ));
        return lessons.stream()
                .map(this::toPublicSummaryDto)
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
        return toPublicDetailDto(publicLesson);
    }

    @Transactional
    public LessonDetailDto createLesson(CreateLessonRequest request, SupabaseAuthUser user) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        requireContributorUser(user);

        String title = trimToNull(request.title());
        Object content = request.content();
        List<UUID> conceptPublicIds = normalizeCreateConceptPublicIds(request.conceptPublicIds());
        UUID contributorId = user.userId();

        if (title == null || content == null || conceptPublicIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title, content, and conceptPublicIds are required"
            );
        }
        if (contributorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user id is required");
        }

        Contributor contributor = contributorRepository.findById(contributorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor not found"));
        List<Concept> concepts = conceptRepository.findAllByPublicIdIn(conceptPublicIds);
        if (concepts.size() != conceptPublicIds.size()) {
            Set<UUID> foundConceptPublicIds = concepts.stream()
                    .map(Concept::getPublicId)
                    .collect(java.util.stream.Collectors.toSet());
            UUID missingConceptPublicId = conceptPublicIds.stream()
                    .filter(id -> !foundConceptPublicIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Concept not found" + (missingConceptPublicId == null ? "" : ": " + missingConceptPublicId)
            );
        }

        boolean submit = Boolean.TRUE.equals(request.submit());
        Lesson lesson = new Lesson(
                title,
                objectMapper.valueToTree(content),
                LessonModerationStatus.UNPUBLISHED,
                contributor,
                OffsetDateTime.now()
        );
        lesson.getConcepts().addAll(concepts);

        Lesson saved = lessonRepository.save(lesson);
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
        requireContributorUser(user);

        String title = trimToNull(request.title());
        Object content = request.content();

        if (title == null || content == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title and content are required"
            );
        }

        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        requireOwner(lesson, user);
        LessonModerationStatus previousStatus = lesson.getLessonModerationStatus();

        lesson.setTitle(title);
        lesson.setContent(objectMapper.valueToTree(content));

        Lesson saved = previousStatus == LessonModerationStatus.APPROVED
                ? lessonModerationWorkflowService.submitForReview(lesson)
                : lessonRepository.save(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto submitLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        requireContributorUser(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        requireOwner(lesson, user);
        LessonModerationStatus status = lesson.getLessonModerationStatus();

        if (status != LessonModerationStatus.UNPUBLISHED && status != LessonModerationStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only UNPUBLISHED or REJECTED lessons can be submitted for review."
            );
        }

        Lesson saved = lessonModerationWorkflowService.submitForReview(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto unpublishLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        requireContributorUser(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        requireOwner(lesson, user);

        Lesson saved = lessonModerationWorkflowService.unpublish(lesson);
        return toDetailDto(saved);
    }

    @Transactional
    public void softDeleteLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }

        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        requireOwner(lesson, user);

        if (lesson.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        if (lesson.getLessonModerationStatus() != LessonModerationStatus.UNPUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only unpublished lessons can be deleted"
            );
        }

        lesson.setDeletedAt(OffsetDateTime.now());
        lessonRepository.save(lesson);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<UUID> normalizeCreateConceptPublicIds(List<UUID> rawConceptPublicIds) {
        if (rawConceptPublicIds == null) {
            return List.of();
        }

        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        for (UUID value : rawConceptPublicIds) {
            if (value != null) {
                ids.add(value);
            }
        }

        return List.copyOf(ids);
    }

    private List<Integer> resolveConceptIdsByPublicIds(List<UUID> conceptPublicIds) {
        if (conceptPublicIds == null || conceptPublicIds.isEmpty()) {
            return List.of();
        }

        List<UUID> normalizedPublicIds = conceptPublicIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedPublicIds.isEmpty()) {
            return List.of();
        }

        List<Concept> concepts = conceptRepository.findAllByPublicIdIn(normalizedPublicIds);
        if (concepts.size() != normalizedPublicIds.size()) {
            Set<UUID> foundPublicIds = concepts.stream()
                    .map(Concept::getPublicId)
                    .collect(java.util.stream.Collectors.toSet());
            UUID missingPublicId = normalizedPublicIds.stream()
                    .filter(id -> !foundPublicIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Concept not found" + (missingPublicId == null ? "" : ": " + missingPublicId)
            );
        }

        return concepts.stream()
                .map(Concept::getConceptId)
                .toList();
    }

    private LessonPublicSummaryDto toPublicSummaryDto(Lesson lesson) {
        return new LessonPublicSummaryDto(
                lesson.getPublicId(),
                lesson.getTitle(),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.conceptSummaries(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getCreatedAt()
        );
    }

    private LessonContributorSummaryDto toContributorSummaryDto(Lesson lesson) {
        return new LessonContributorSummaryDto(
                lesson.getPublicId(),
                lesson.getTitle(),
                lesson.getLessonModerationStatus().name(),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.conceptSummaries(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getCreatedAt()
        );
    }

    private LessonDetailDto toDetailDto(Lesson lesson) {
        LessonDetailBase base = toDetailBase(lesson);
        LessonModerationRecord latestRecord = lessonModerationRecordRepository
                .findTopByLessonOrderByRecordedAtDesc(lesson)
                .orElse(null);
        LessonModerationRecord latestAdminRecord = lessonModerationRecordRepository
                .findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(lesson, LessonModerationDecisionSource.ADMIN)
                .orElse(null);
        return new LessonDetailDto(
                base.lessonPublicId(),
                base.title(),
                base.content(),
                lesson.getLessonModerationStatus().name(),
                base.conceptPublicIds(),
                base.concepts(),
                base.author(),
                base.createdAt(),
                latestModerationReasons(latestRecord),
                latestModerationEventType(latestRecord),
                latestModeratedAt(latestRecord),
                adminRejectionReason(latestAdminRecord)
        );
    }

    private LessonPublicDetailDto toPublicDetailDto(Lesson lesson) {
        LessonDetailBase base = toDetailBase(lesson);
        return new LessonPublicDetailDto(
                base.lessonPublicId(),
                base.title(),
                base.content(),
                base.conceptPublicIds(),
                base.concepts(),
                base.author(),
                base.createdAt()
        );
    }

    private LessonDetailBase toDetailBase(Lesson lesson) {
        return new LessonDetailBase(
                lesson.getPublicId(),
                lesson.getTitle(),
                objectMapper.convertValue(lesson.getContent(), Object.class),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.conceptSummaries(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getCreatedAt()
        );
    }

    private List<String> latestModerationReasons(LessonModerationRecord latestRecord) {
        if (latestRecord == null || latestRecord.getReasons() == null || latestRecord.getReasons().isNull()) {
            return List.of();
        }
        return objectMapper.convertValue(latestRecord.getReasons(), new TypeReference<List<String>>() {});
    }

    private String latestModerationEventType(LessonModerationRecord latestRecord) {
        return latestRecord == null || latestRecord.getEventType() == null
                ? null
                : latestRecord.getEventType().name();
    }

    private OffsetDateTime latestModeratedAt(LessonModerationRecord latestRecord) {
        return latestRecord == null ? null : latestRecord.getRecordedAt();
    }

    private String adminRejectionReason(LessonModerationRecord latestAdminRecord) {
        if (latestAdminRecord == null || latestAdminRecord.getEventType() != LessonModerationEventType.ADMIN_REJECTED) {
            return null;
        }

        return latestAdminRecord.getReviewNote();
    }

    private record LessonDetailBase(
            UUID lessonPublicId,
            String title,
            Object content,
            List<UUID> conceptPublicIds,
            List<LessonConceptSummaryDto> concepts,
            LessonAuthorDto author,
            OffsetDateTime createdAt
    ) {}

    private void requireContributorUser(SupabaseAuthUser user) {
        if (user == null || !user.isContributor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contributor access required");
        }
    }

    private void requireOwner(Lesson lesson, SupabaseAuthUser user) {
        UUID ownerId = lesson.getContributor() == null ? null : lesson.getContributor().getContributorId();
        if (ownerId == null || !ownerId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson owner access required");
        }
    }
}
