package com.example.demo.admin.lesson;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.query.ConceptsMatchMode;

@RestController()
@RequestMapping("/api/admin/lessons")
public class AdminLessonController {
    private final AdminLessonFacade adminFacade;

    public AdminLessonController(AdminLessonFacade adminFacade){
        this.adminFacade = adminFacade;
    }

    @GetMapping
    public List<AdminLessonSummaryDto> getAllLessonsForAdmin(
            @RequestParam(required = false) List<UUID> conceptPublicIds,
            @RequestParam(defaultValue = "any") String conceptsMatch,
            @RequestParam(required = false) LessonModerationStatus status
    ) {
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return adminFacade.getAllLessons(conceptPublicIds, matchMode, status);
    }

    @GetMapping("/{lessonPublicId}")
    public AdminLessonReviewDto getLessonForAdmin(@PathVariable UUID lessonPublicId) {
        return adminFacade.getLessonById(lessonPublicId);
    }

    @PutMapping("/{lessonPublicId}/approve")
    public AdminLessonDetailDto approveLesson(@PathVariable UUID lessonPublicId){
        return adminFacade.approveLesson(lessonPublicId);
    }

    @PutMapping("/{lessonPublicId}/reject")
    public AdminLessonDetailDto rejectLesson(@PathVariable UUID lessonPublicId){
        return adminFacade.rejectLesson(lessonPublicId);
    }

}
