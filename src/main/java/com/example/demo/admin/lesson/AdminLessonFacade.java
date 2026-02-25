package com.example.demo.admin.lesson;

import java.time.OffsetDateTime;
import java.util.List;

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
    private final ObjectMapper objectMapper;

    public AdminLessonFacade(
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService,
            LessonMappingSupport lessonMappingSupport,
            LessonListQueryService lessonListQueryService,
            ObjectMapper objectMapper
    ){
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
        this.lessonMappingSupport = lessonMappingSupport;
        this.lessonListQueryService = lessonListQueryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminLessonSummaryDto> getAllLessons(
            List<Integer> conceptIds,
            ConceptsMatchMode conceptsMatch,
            LessonModerationStatus status
    ) {
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
    public AdminLessonReviewDto getLessonById(Integer id) {
        Lesson lesson = lessonLookupService.findByIdOrThrow(id);
        return new AdminLessonReviewDto(
                lesson.getLessonId(),
                lesson.getTitle(),
                objectMapper.convertValue(lesson.getContent(), Object.class),
                lessonMappingSupport.conceptIds(lesson),
                lessonMappingSupport.contributorId(lesson),
                lesson.getLessonModerationStatus(),
                lesson.getCreatedAt(),
                lesson.getDeletedAt()
        );
    }

    public AdminLessonDetailDto approveLesson(Integer id){
        Lesson lesson = lessonLookupService.findByIdOrThrow(id);
        Lesson saved = lessonModerationWorkflowService.approve(lesson);
        return toAdminLessonDetailDto(saved);
    }

    public AdminLessonDetailDto rejectLesson(Integer id){
        Lesson lesson = lessonLookupService.findByIdOrThrow(id);
        Lesson saved = lessonModerationWorkflowService.reject(lesson);
        return toAdminLessonDetailDto(saved);
    }

    private AdminLessonDetailDto toAdminLessonDetailDto(Lesson lesson) {
        return new AdminLessonDetailDto(
                lessonMappingSupport.contributorId(lesson),
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getLessonModerationStatus()
        );
    }

    private AdminLessonSummaryDto toAdminLessonSummaryDto(Lesson lesson) {
        OffsetDateTime deletedAt = lesson.getDeletedAt();
        return new AdminLessonSummaryDto(
                lesson.getLessonId(),
                lesson.getTitle(),
                lessonMappingSupport.conceptIds(lesson),
                lessonMappingSupport.contributorId(lesson),
                lesson.getLessonModerationStatus(),
                lesson.getCreatedAt(),
                deletedAt
        );
    }
}
