package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lesson.dto.LessonConceptSummaryDto;
import com.example.demo.lesson.dto.LessonContributorSummaryDto;
import com.example.demo.lesson.dto.LessonDetailDto;
import com.example.demo.lesson.dto.LessonPublicDetailDto;
import com.example.demo.lesson.dto.LessonPublicSummaryDto;
import com.example.demo.lesson.moderation.LessonModerationDecisionSource;
import com.example.demo.lesson.moderation.LessonModerationEventType;
import com.example.demo.lesson.moderation.LessonModerationRecord;
import com.example.demo.lesson.moderation.LessonModerationRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class LessonDetailAssembler {

    private final LessonMappingSupport lessonMappingSupport;
    private final LessonModerationRecordRepository lessonModerationRecordRepository;
    private final LessonSectionService lessonSectionService;
    private final com.example.demo.lesson.enrollment.LessonEnrollmentService lessonEnrollmentService;
    private final ObjectMapper objectMapper;

    LessonDetailAssembler(
            LessonMappingSupport lessonMappingSupport,
            LessonModerationRecordRepository lessonModerationRecordRepository,
            LessonSectionService lessonSectionService,
            com.example.demo.lesson.enrollment.LessonEnrollmentService lessonEnrollmentService,
            ObjectMapper objectMapper
    ) {
        this.lessonMappingSupport = lessonMappingSupport;
        this.lessonModerationRecordRepository = lessonModerationRecordRepository;
        this.lessonSectionService = lessonSectionService;
        this.lessonEnrollmentService = lessonEnrollmentService;
        this.objectMapper = objectMapper;
    }

    LessonPublicSummaryDto toPublicSummaryDto(Lesson lesson) {
        return new LessonPublicSummaryDto(
                lesson.getPublicId(),
                lesson.getTitle(),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.conceptSummaries(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getCreatedAt());
    }

    LessonContributorSummaryDto toContributorSummaryDto(Lesson lesson) {
        UUID lessonPublicId = lesson.getPublicId();
        long enrollmentCount = lessonEnrollmentService.countEnrollments(lessonPublicId);
        long completionCount = lessonEnrollmentService.countCompletions(lessonPublicId);
        return new LessonContributorSummaryDto(
                lessonPublicId,
                lesson.getTitle(),
                lesson.getLessonModerationStatus().name(),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.conceptSummaries(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getCreatedAt(),
                enrollmentCount,
                completionCount);
    }

    LessonDetailDto toDetailDto(Lesson lesson) {
        LessonDetailBase base = toDetailBase(lesson);
        LessonModerationRecord latestRecord = lessonModerationRecordRepository
                .findTopByLessonOrderByRecordedAtDesc(lesson)
                .orElse(null);
        LessonModerationRecord latestAdminRecord = lessonModerationRecordRepository
                .findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(lesson, LessonModerationDecisionSource.ADMIN)
                .orElse(null);
        List<LessonSection> sections = lessonSectionService.getSectionsForLesson(lesson);

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
                adminRejectionReason(latestAdminRecord),
                lessonSectionService.toSectionDtos(sections),
                sections.size());
    }

    LessonPublicDetailDto toPublicDetailDto(Lesson lesson, boolean enrolled) {
        LessonDetailBase base = toDetailBase(lesson);
        List<LessonSection> sections = enrolled ? lessonSectionService.getSectionsForLesson(lesson) : List.of();
        return new LessonPublicDetailDto(
                base.lessonPublicId(),
                base.title(),
                base.content(),
                base.conceptPublicIds(),
                base.concepts(),
                base.author(),
                base.createdAt(),
                lessonSectionService.toSectionDtos(sections),
                sections.size(),
                enrolled);
    }

    private LessonDetailBase toDetailBase(Lesson lesson) {
        return new LessonDetailBase(
                lesson.getPublicId(),
                lesson.getTitle(),
                objectMapper.convertValue(lesson.getContent(), Object.class),
                lessonMappingSupport.conceptPublicIds(lesson),
                lessonMappingSupport.conceptSummaries(lesson),
                lessonMappingSupport.author(lesson),
                lesson.getCreatedAt());
    }

    private List<String> latestModerationReasons(LessonModerationRecord latestRecord) {
        if (latestRecord == null || latestRecord.getReasons() == null || latestRecord.getReasons().isNull()) {
            return List.of();
        }
        return objectMapper.convertValue(latestRecord.getReasons(), new TypeReference<List<String>>() {
        });
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
            OffsetDateTime createdAt) {
    }
}
