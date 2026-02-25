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
import com.example.demo.lesson.dto.request.CreateLessonRequest;
import com.example.demo.lesson.dto.request.UpdateLessonRequest;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.dto.response.LessonContributorSummaryDto;
import com.example.demo.lesson.dto.response.LessonDetailDto;
import com.example.demo.lesson.dto.response.LessonDetailView;
import com.example.demo.lesson.dto.response.LessonPublicDetailDto;
import com.example.demo.lesson.dto.response.LessonPublicSummaryDto;
import com.example.demo.lesson.enums.LessonModerationStatus;
import com.example.demo.lesson.query.ConceptsMatchMode;
import com.example.demo.lesson.query.LessonListAudience;
import com.example.demo.lesson.query.LessonListCriteria;
import com.example.demo.lesson.query.LessonListQueryService;
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
    private final ObjectMapper objectMapper;
    public LessonService(
            LessonRepository lessonRepository,
            ContributorRepository contributorRepository,
            ConceptRepository conceptRepository,
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService,
            LessonMappingSupport lessonMappingSupport,
            LessonListQueryService lessonListQueryService,
            ObjectMapper objectMapper
    ) {
        this.lessonRepository = lessonRepository;
        this.contributorRepository = contributorRepository;
        this.conceptRepository = conceptRepository;
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
        this.lessonMappingSupport = lessonMappingSupport;
        this.lessonListQueryService = lessonListQueryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LessonContributorSummaryDto> getLessonsByContributor(
            UUID contributorId,
            List<Integer> conceptIds,
            ConceptsMatchMode conceptsMatch
    ) {
        List<Lesson> lessons = lessonListQueryService.findLessons(new LessonListCriteria(
                conceptIds,
                conceptsMatch,
                contributorId,
                null,
                LessonListAudience.CONTRIBUTOR
        ));

        return lessons.stream()
                .map(this::toContributorSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonPublicSummaryDto> findPublicLessons(List<Integer> conceptIds, ConceptsMatchMode conceptsMatch) {
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
    public LessonDetailView getLessonDetailForUser(Integer lessonId, SupabaseAuthUser user) {
        Lesson lesson = lessonLookupService.findByIdOrThrow(lessonId);
        if (user != null && user.userId() != null
                && lesson.getContributor() != null
                && lesson.getContributor().getContributorId().equals(user.userId())
                && lesson.getDeletedAt() == null) {
            return toDetailDto(lesson);
        }

        Lesson publicLesson = lessonLookupService.findPublicByIdOrThrow(lessonId);
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
        List<Integer> conceptIds = normalizeCreateConceptIds(request.conceptIds());
        UUID contributorId = user.userId();

        if (title == null || content == null || conceptIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title, content, and conceptId (number or array) are required"
            );
        }
        if (contributorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user id is required");
        }

        Contributor contributor = contributorRepository.findById(contributorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor not found"));
        List<Concept> concepts = conceptRepository.findAllById(conceptIds);
        if (concepts.size() != conceptIds.size()) {
            Set<Integer> foundConceptIds = concepts.stream()
                    .map(Concept::getConceptId)
                    .collect(java.util.stream.Collectors.toSet());
            Integer missingConceptId = conceptIds.stream()
                    .filter(id -> !foundConceptIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Concept not found" + (missingConceptId == null ? "" : ": " + missingConceptId)
            );
        }

        boolean submit = Boolean.TRUE.equals(request.submit());
        Lesson lesson = new Lesson(
                title,
                objectMapper.valueToTree(content),
                submit ? LessonModerationStatus.PENDING : LessonModerationStatus.UNPUBLISHED,
                contributor,
                OffsetDateTime.now()
        );
        lesson.getConcepts().addAll(concepts);

        Lesson saved = lessonRepository.save(lesson);

        return toDetailDto(saved);
    }

    public LessonDetailDto updateLesson(Integer lessonId, UpdateLessonRequest request, SupabaseAuthUser user) {
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

        Lesson lesson = lessonLookupService.findByIdOrThrow(lessonId);
        requireOwner(lesson, user);

        lesson.setTitle(title);
        lesson.setContent(objectMapper.valueToTree(content));

        Lesson saved = lessonRepository.save(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto submitLesson(Integer lessonId, SupabaseAuthUser user) {
        requireContributorUser(user);
        Lesson lesson = lessonLookupService.findByIdOrThrow(lessonId);
        requireOwner(lesson, user);

        Lesson saved = lessonModerationWorkflowService.submitForReview(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto unpublishLesson(Integer lessonId, SupabaseAuthUser user) {
        requireContributorUser(user);
        Lesson lesson = lessonLookupService.findByIdOrThrow(lessonId);
        requireOwner(lesson, user);

        Lesson saved = lessonModerationWorkflowService.unpublish(lesson);
        return toDetailDto(saved);
    }

    @Transactional
    public void softDeleteLesson(Integer lessonId, SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }

        Lesson lesson = lessonLookupService.findByIdOrThrow(lessonId);
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

    private List<Integer> normalizeCreateConceptIds(Object rawConceptId) {
        if (rawConceptId == null) {
            return List.of();
        }

        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        if (rawConceptId instanceof List<?> rawList) {
            for (Object value : rawList) {
                if (value instanceof Number number) {
                    ids.add(number.intValue());
                }
            }
        } else if (rawConceptId instanceof Number number) {
            ids.add(number.intValue());
        }

        return List.copyOf(ids);
    }

    private LessonPublicSummaryDto toPublicSummaryDto(Lesson lesson) {
        return new LessonPublicSummaryDto(
                lesson.getLessonId(),
                lesson.getTitle(),
                lessonMappingSupport.conceptIds(lesson),
                lessonMappingSupport.contributorId(lesson),
                lesson.getCreatedAt()
        );
    }

    private LessonContributorSummaryDto toContributorSummaryDto(Lesson lesson) {
        return new LessonContributorSummaryDto(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getLessonModerationStatus().name(),
                lessonMappingSupport.conceptIds(lesson),
                lessonMappingSupport.contributorId(lesson),
                lesson.getCreatedAt()
        );
    }

    private LessonDetailDto toDetailDto(Lesson lesson) {
        LessonDetailBase base = toDetailBase(lesson);
        return new LessonDetailDto(
                base.lessonId(),
                base.title(),
                base.content(),
                lesson.getLessonModerationStatus().name(),
                base.conceptIds(),
                base.contributorId(),
                base.createdAt()
        );
    }

    private LessonPublicDetailDto toPublicDetailDto(Lesson lesson) {
        LessonDetailBase base = toDetailBase(lesson);
        return new LessonPublicDetailDto(
                base.lessonId(),
                base.title(),
                base.content(),
                base.conceptIds(),
                base.contributorId(),
                base.createdAt()
        );
    }

    private LessonDetailBase toDetailBase(Lesson lesson) {
        return new LessonDetailBase(
                lesson.getLessonId(),
                lesson.getTitle(),
                objectMapper.convertValue(lesson.getContent(), Object.class),
                lessonMappingSupport.conceptIds(lesson),
                lessonMappingSupport.contributorId(lesson),
                lesson.getCreatedAt()
        );
    }

    private record LessonDetailBase(
            Integer lessonId,
            String title,
            Object content,
            List<Integer> conceptIds,
            UUID contributorId,
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
