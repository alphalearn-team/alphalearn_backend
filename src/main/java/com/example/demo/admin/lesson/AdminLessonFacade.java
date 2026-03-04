package com.example.demo.admin.lesson;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonMappingSupport;
import com.example.demo.lesson.LessonModerationWorkflowService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.query.ConceptsMatchMode;
import com.example.demo.lesson.query.LessonListAudience;
import com.example.demo.lesson.query.LessonListCriteria;
import com.example.demo.lesson.query.LessonListQueryService;

@Service
public class AdminLessonFacade {
    private final LessonLookupService lessonLookupService;
    private final LessonModerationWorkflowService lessonModerationWorkflowService;
    private final LessonMappingSupport lessonMappingSupport;
    private final LessonListQueryService lessonListQueryService;
    private final ConceptRepository conceptRepository;
    private final LessonModerationRecordRepository lessonModerationRecordRepository;
    private final ObjectMapper objectMapper;

    public AdminLessonFacade(
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService,
            LessonMappingSupport lessonMappingSupport,
            LessonListQueryService lessonListQueryService,
            ConceptRepository conceptRepository,
            LessonModerationRecordRepository lessonModerationRecordRepository,
            ObjectMapper objectMapper
    ){
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
        this.lessonMappingSupport = lessonMappingSupport;
        this.lessonListQueryService = lessonListQueryService;
        this.conceptRepository = conceptRepository;
        this.lessonModerationRecordRepository = lessonModerationRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminLessonSummaryDto> getAllLessons(
            List<UUID> conceptPublicIds,
            ConceptsMatchMode conceptsMatch,
            LessonModerationStatus status
    ) {
        List<Integer> conceptIds = resolveConceptIdsByPublicIds(conceptPublicIds);
        List<Lesson> lessons = lessonListQueryService.findLessons(new LessonListCriteria(
                conceptIds,
                conceptsMatch,
                null,
                status,
                LessonListAudience.ADMIN
        ));

        return lessons.stream()
                .map(this::toAdminLessonSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminLessonReviewDto getLessonByPublicId(UUID lessonPublicId) {
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        LessonModerationRecord automatedRecord = latestAutomatedRecord(lesson);
        LessonModerationRecord adminRecord = latestAdminRecord(lesson);
        return new AdminLessonReviewDto(
                lesson.getPublicId(),
                lesson.getTitle(),
                objectMapper.convertValue(lesson.getContent(), Object.class),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getLessonModerationStatus(),
                toReasons(automatedRecord),
                adminRejectionReason(adminRecord),
                lesson.getCreatedAt(),
                lesson.getDeletedAt()
        );
    }

    public AdminLessonDetailDto approveLesson(UUID lessonPublicId){
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        Lesson saved = lessonModerationWorkflowService.approve(lesson);
        return toAdminLessonDetailDto(saved);
    }

    public AdminLessonDetailDto rejectLesson(UUID lessonPublicId){
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        Lesson saved = lessonModerationWorkflowService.reject(lesson);
        return toAdminLessonDetailDto(saved);
    }

    private AdminLessonDetailDto toAdminLessonDetailDto(Lesson lesson) {
        return new AdminLessonDetailDto(
                lessonMappingSupport.author(lesson),
                lesson.getPublicId(),
                lesson.getTitle(),
                lesson.getLessonModerationStatus()
        );
    }

    private AdminLessonSummaryDto toAdminLessonSummaryDto(Lesson lesson) {
        OffsetDateTime deletedAt = lesson.getDeletedAt();
        return new AdminLessonSummaryDto(
                lesson.getPublicId(),
                lesson.getTitle(),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getLessonModerationStatus(),
                lesson.getCreatedAt(),
                deletedAt
        );
    }

    private LessonModerationRecord latestAutomatedRecord(Lesson lesson) {
        return lessonModerationRecordRepository.findTopByLessonAndDecisionSourceInOrderByRecordedAtDesc(
                lesson,
                EnumSet.of(LessonModerationDecisionSource.AUTO, LessonModerationDecisionSource.AUTO_FALLBACK)
        ).orElse(null);
    }

    private LessonModerationRecord latestAdminRecord(Lesson lesson) {
        return lessonModerationRecordRepository.findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(
                lesson,
                LessonModerationDecisionSource.ADMIN
        ).orElse(null);
    }

    private List<String> toReasons(LessonModerationRecord record) {
        if (record == null || record.getReasons() == null || record.getReasons().isNull()) {
            return List.of();
        }

        return objectMapper.convertValue(record.getReasons(), new TypeReference<List<String>>() {});
    }

    private String adminRejectionReason(LessonModerationRecord adminRecord) {
        if (adminRecord == null || adminRecord.getEventType() != LessonModerationEventType.ADMIN_REJECTED) {
            return null;
        }

        return adminRecord.getReviewNote();
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
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Concept not found" + (missingPublicId == null ? "" : ": " + missingPublicId)
            );
        }

        return concepts.stream()
                .map(Concept::getConceptId)
                .toList();
    }
}
