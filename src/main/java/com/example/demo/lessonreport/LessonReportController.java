package com.example.demo.lessonreport;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lessonreport.dto.CreateLessonReportRequest;
import com.example.demo.lessonreport.dto.LessonReportResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/lesson-reports")
@Tag(name = "Lesson Reports", description = "Learner and contributor lesson reporting endpoints")
public class LessonReportController {

    private final LessonReportService lessonReportService;

    public LessonReportController(LessonReportService lessonReportService) {
        this.lessonReportService = lessonReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a lesson", description = "Creates a lesson report for an authenticated learner or contributor")
    public LessonReportResponseDto createLessonReport(
            @RequestBody CreateLessonReportRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonReportService.createReport(request, user);
    }
}
