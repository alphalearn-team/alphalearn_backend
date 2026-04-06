package com.example.demo.admin.lessonreport;

import java.util.List;

import com.example.demo.admin.lesson.AdminLessonReviewDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminReportedLessonDetail", description = "Admin detail payload for a lesson with pending reports")
public record AdminReportedLessonDetailDto(
        @Schema(description = "Existing admin lesson review payload")
        AdminLessonReviewDto lesson,
        @Schema(description = "Pending lesson report entries shown in the left panel")
        List<AdminPendingLessonReportReasonDto> pendingReports,
        @Schema(description = "Total pending report count")
        long pendingReportCount
) {}
