package com.example.demo.admin.lesson;

import java.util.List;

import com.example.demo.admin.lesson.dto.AdminLessonDetailDto;
import com.example.demo.admin.lesson.dto.AdminLessonSummaryDto;
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
    private final AdminLessonService adminService;

    public AdminLessonController(AdminLessonService adminService){
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminLessonSummaryDto> getAllLessonsForAdmin(
            @RequestParam(required = false) List<Integer> conceptIds,
            @RequestParam(defaultValue = "any") String conceptsMatch,
            @RequestParam(required = false) LessonModerationStatus status
    ) {
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return adminService.getAllLessons(conceptIds, matchMode, status);
    }

    @PutMapping("/{id}/approve")
    public AdminLessonDetailDto approveLesson(@PathVariable Integer id){
        return adminService.approveLesson(id);
    }

    @PutMapping("/{id}/reject")
    public AdminLessonDetailDto rejectLesson(@PathVariable Integer id){
        return adminService.rejectLesson(id);
    }

}
