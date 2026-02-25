package com.example.demo.admin;

import org.springframework.stereotype.Service;

import com.example.demo.admin.dto.response.AdminLessonDetailDto;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonMappingSupport;
import com.example.demo.lesson.LessonModerationWorkflowService;

@Service
public class AdminLessonService {
    private final LessonLookupService lessonLookupService;
    private final LessonModerationWorkflowService lessonModerationWorkflowService;
    private final LessonMappingSupport lessonMappingSupport;

    public AdminLessonService(
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService,
            LessonMappingSupport lessonMappingSupport
    ){
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
        this.lessonMappingSupport = lessonMappingSupport;
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
}
