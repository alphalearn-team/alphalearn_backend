package com.example.demo.admin.lessonreport;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.admin.lesson.AdminLessonReviewDto;
import com.example.demo.admin.lesson.AdminLessonService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lessonreport.LessonReport;
import com.example.demo.lessonreport.LessonReportRepository;
import com.example.demo.lessonreport.LessonReportStatus;

@Service
public class AdminLessonReportService {

    private final LessonReportRepository lessonReportRepository;
    private final AdminLessonService adminLessonService;

    public AdminLessonReportService(
            LessonReportRepository lessonReportRepository,
            AdminLessonService adminLessonService
    ) {
        this.lessonReportRepository = lessonReportRepository;
        this.adminLessonService = adminLessonService;
    }

    @Transactional(readOnly = true)
    public List<AdminReportedLessonSummaryDto> listPendingReportedLessons() {
        return lessonReportRepository.findPendingLessonAggregates().stream()
                .map(row -> new AdminReportedLessonSummaryDto(
                        row.getLessonPublicId(),
                        row.getTitle(),
                        new LessonAuthorDto(row.getAuthorPublicId(), row.getAuthorUsername()),
                        LessonModerationStatus.valueOf(row.getLessonModerationStatus()),
                        row.getPendingReportCount(),
                        row.getLatestReason(),
                        row.getLatestReportedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminReportedLessonDetailDto getPendingReportedLessonDetail(UUID lessonPublicId) {
        AdminLessonReviewDto lesson = adminLessonService.getLessonByPublicId(lessonPublicId);
        List<LessonReport> pendingReports = lessonReportRepository.findByLesson_PublicIdAndStatusOrderByCreatedAtDesc(
                lessonPublicId,
                LessonReportStatus.PENDING
        );

        List<AdminPendingLessonReportReasonDto> reportDtos = pendingReports.stream()
                .map(report -> new AdminPendingLessonReportReasonDto(
                        report.getPublicId(),
                        report.getReason(),
                        report.getCreatedAt()
                ))
                .toList();

        return new AdminReportedLessonDetailDto(lesson, reportDtos, reportDtos.size());
    }
}
