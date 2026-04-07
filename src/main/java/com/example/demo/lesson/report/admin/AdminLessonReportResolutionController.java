package com.example.demo.lesson.report.admin;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/lesson-reports")
@Tag(name = "Admin Lesson Reports", description = "Admin endpoints for reviewing reported lessons")
public class AdminLessonReportResolutionController {

    private final AdminLessonReportService adminLessonReportService;

    public AdminLessonReportResolutionController(AdminLessonReportService adminLessonReportService) {
        this.adminLessonReportService = adminLessonReportService;
    }

    @DeleteMapping("/{reportPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dismiss one pending report", description = "Marks one pending report as dismissed")
    public void dismissPendingReport(
            @PathVariable UUID reportPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        adminLessonReportService.dismissPendingReport(reportPublicId, user);
    }
}
