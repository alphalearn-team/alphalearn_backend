package com.example.demo.lesson;

import java.time.OffsetDateTime;
import java.util.List;
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
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ContributorRepository contributorRepository;
    private final ConceptRepository conceptRepository;
    private final ObjectMapper objectMapper;
    public LessonService(
            LessonRepository lessonRepository,
            ContributorRepository contributorRepository,
            ConceptRepository conceptRepository,
            ObjectMapper objectMapper
    ) {
        this.lessonRepository = lessonRepository;
        this.contributorRepository = contributorRepository;
        this.conceptRepository = conceptRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LessonPublicSummaryDto>  findAllLessons() {
        List<Lesson> lessons = lessonRepository.findByLessonModerationStatusAndDeletedAtIsNull(LessonModerationStatus.APPROVED);
        return lessons.stream()
                .map(this::toPublicSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonContributorSummaryDto> getLessonsByContributor(
            UUID contributorId,
            List<Integer> conceptIds,
            String conceptsMatch
    ) {
        if (!contributorRepository.existsById(contributorId)) {
            return List.of();
        }

        List<Lesson> lessons;
        if (conceptIds == null || conceptIds.isEmpty()) {
            lessons = lessonRepository.findByContributor_ContributorIdAndDeletedAtIsNull(contributorId);
        } else if ("any".equals(conceptsMatch)) {
            lessons = lessonRepository.findByContributorAndConceptIds(contributorId, conceptIds);
        } else {
            lessons = lessonRepository.findByContributorAndAllConceptIds(contributorId, conceptIds, conceptIds.size());
        }

        return lessons.stream()
                .map(this::toContributorSummaryDto)
                .toList();
    }

    // lessons that match at least one of the given concepts
    @Transactional(readOnly = true)
    public List<LessonPublicSummaryDto> getLessonsByConcepts(List<Integer> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }

        List<Lesson> lessons = lessonRepository.findByConceptIds(conceptIds);
        return lessons.stream()
                .map(this::toPublicSummaryDto)
                .toList();
    }

    // lessons that match all of the given concepts
    @Transactional(readOnly = true)
    public List<LessonPublicSummaryDto> getLessonsByAllConcepts(List<Integer> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }

        List<Lesson> lessons = lessonRepository.findByAllConceptIds(
                conceptIds,
                (int) conceptIds.size()
        );

        return lessons.stream()
                .map(this::toPublicSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonDetailView getLessonDetailForUser(Integer lessonId, SupabaseAuthUser user) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        if (user != null && user.userId() != null
                && lesson.getContributor() != null
                && lesson.getContributor().getContributorId().equals(user.userId())
                && lesson.getDeletedAt() == null) {
            return toDetailDto(lesson);
        }

        Lesson publicLesson = lessonRepository.findPublicById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
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
        Integer conceptId = request.conceptId();
        UUID contributorId = request.contributorId();

        if (title == null || content == null || conceptId == null || contributorId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title, content, conceptId, and contributorId are required"
            );
        }
        if (!contributorId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create lesson for another contributor");
        }

        Contributor contributor = contributorRepository.findById(contributorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor not found"));
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found"));

        boolean submit = Boolean.TRUE.equals(request.submit());
        Lesson lesson = new Lesson(
                title,
                objectMapper.valueToTree(content),
                submit ? LessonModerationStatus.PENDING : LessonModerationStatus.UNPUBLISHED,
                contributor,
                OffsetDateTime.now()
        );
        lesson.getConcepts().add(concept);

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

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        requireOwner(lesson, user);

        lesson.setTitle(title);
        lesson.setContent(objectMapper.valueToTree(content));

        Lesson saved = lessonRepository.save(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto submitLesson(Integer lessonId, SupabaseAuthUser user) {
        requireContributorUser(user);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        requireOwner(lesson, user);

        lesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
        Lesson saved = lessonRepository.save(lesson);
        return toDetailDto(saved);
    }

    public LessonDetailDto unpublishLesson(Integer lessonId, SupabaseAuthUser user) {
        requireContributorUser(user);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        requireOwner(lesson, user);

        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        Lesson saved = lessonRepository.save(lesson);
        return toDetailDto(saved);
    }

    @Transactional
    public void softDeleteLesson(Integer lessonId, SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
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

    private LessonPublicSummaryDto toPublicSummaryDto(Lesson lesson) {
        return new LessonPublicSummaryDto(
                lesson.getLessonId(),
                lesson.getTitle(),
                toConceptIds(lesson),
                lesson.getContributor().getContributorId(),
                lesson.getCreatedAt()
        );
    }

    private LessonContributorSummaryDto toContributorSummaryDto(Lesson lesson) {
        return new LessonContributorSummaryDto(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getLessonModerationStatus().name(),
                toConceptIds(lesson),
                lesson.getContributor().getContributorId(),
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
                toConceptIds(lesson),
                lesson.getContributor().getContributorId(),
                lesson.getCreatedAt()
        );
    }

    private List<Integer> toConceptIds(Lesson lesson) {
        if (lesson.getConcepts() == null || lesson.getConcepts().isEmpty()) {
            return List.of();
        }
        return lesson.getConcepts().stream()
                .map(Concept::getConceptId)
                .sorted()
                .toList();
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
