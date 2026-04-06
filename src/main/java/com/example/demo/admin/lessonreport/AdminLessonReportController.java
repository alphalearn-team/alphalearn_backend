package com.example.demo.admin.lessonreport;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/lesson-reports/lessons")
@Tag(name = "Admin Lesson Reports", description = "Admin endpoints for reviewing reported lessons")
public class AdminLessonReportController {

    private final AdminLessonReportService adminLessonReportService;

    public AdminLessonReportController(AdminLessonReportService adminLessonReportService) {
        this.adminLessonReportService = adminLessonReportService;
    }

    @GetMapping
    @Operation(summary = "List reported lessons", description = "Returns lessons that have at least one pending report")
    public List<AdminReportedLessonSummaryDto> listPendingReportedLessons() {
        return adminLessonReportService.listPendingReportedLessons();
    }

    @GetMapping("/{lessonPublicId}")
    @Operation(summary = "Get reported lesson detail", description = "Returns lesson review payload together with pending report reasons")
    public AdminReportedLessonDetailDto getPendingReportedLessonDetail(@PathVariable UUID lessonPublicId) {
        return adminLessonReportService.getPendingReportedLessonDetail(lessonPublicId);
    }
}
