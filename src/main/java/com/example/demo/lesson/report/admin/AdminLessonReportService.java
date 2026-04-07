package com.example.demo.lesson.report.admin;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.lesson.admin.AdminLessonReviewDto;
import com.example.demo.lesson.admin.AdminLessonService;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.read.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lesson.report.LessonReport;
import com.example.demo.lesson.report.LessonReportResolutionAction;
import com.example.demo.lesson.report.LessonReportRepository;
import com.example.demo.lesson.report.LessonReportStatus;

@Service
public class AdminLessonReportService {

    private final LessonReportRepository lessonReportRepository;
    private final AdminLessonService adminLessonService;
    private final LessonLookupService lessonLookupService;

    public AdminLessonReportService(
            LessonReportRepository lessonReportRepository,
            AdminLessonService adminLessonService,
            LessonLookupService lessonLookupService
    ) {
        this.lessonReportRepository = lessonReportRepository;
        this.adminLessonService = adminLessonService;
        this.lessonLookupService = lessonLookupService;
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
                        row.getLatestReportedAt() == null ? null : OffsetDateTime.ofInstant(row.getLatestReportedAt(), ZoneOffset.UTC)
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

    @Transactional
    public void dismissPendingReport(UUID reportPublicId, SupabaseAuthUser user) {
        UUID actorUserId = requireActorUserId(user);
        int resolvedCount = lessonReportRepository.resolvePendingByReportPublicId(
                reportPublicId,
                LessonReportStatus.PENDING,
                LessonReportStatus.RESOLVED,
                OffsetDateTime.now(),
                actorUserId,
                LessonReportResolutionAction.DISMISSED
        );
        if (resolvedCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending report not found");
        }
    }

    @Transactional
    public void dismissPendingReportsForLesson(UUID lessonPublicId, SupabaseAuthUser user) {
        UUID actorUserId = requireActorUserId(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        lessonReportRepository.resolvePendingForLessonId(
                lesson.getLessonId(),
                LessonReportStatus.PENDING,
                LessonReportStatus.RESOLVED,
                OffsetDateTime.now(),
                actorUserId,
                LessonReportResolutionAction.DISMISSED
        );
    }

    private UUID requireActorUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated admin user required");
        }
        return user.userId();
    }
}
