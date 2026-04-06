package com.example.demo.admin.lessonreport;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.admin.lesson.AdminLessonReviewDto;
import com.example.demo.admin.lesson.AdminLessonService;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.LessonModerationWorkflowService;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lessonreport.LessonReport;
import com.example.demo.lessonreport.LessonReportResolutionAction;
import com.example.demo.lessonreport.LessonReportRepository;
import com.example.demo.lessonreport.LessonReportStatus;

@Service
public class AdminLessonReportService {

    private final LessonReportRepository lessonReportRepository;
    private final AdminLessonService adminLessonService;
    private final LessonLookupService lessonLookupService;
    private final LessonModerationWorkflowService lessonModerationWorkflowService;

    public AdminLessonReportService(
            LessonReportRepository lessonReportRepository,
            AdminLessonService adminLessonService,
            LessonLookupService lessonLookupService,
            LessonModerationWorkflowService lessonModerationWorkflowService
    ) {
        this.lessonReportRepository = lessonReportRepository;
        this.adminLessonService = adminLessonService;
        this.lessonLookupService = lessonLookupService;
        this.lessonModerationWorkflowService = lessonModerationWorkflowService;
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
    public AdminLessonReportResolutionResultDto dismissPendingReports(UUID lessonPublicId, SupabaseAuthUser user) {
        UUID actorUserId = requireActorUserId(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        int resolvedCount = resolvePendingReports(
                lesson.getLessonId(),
                actorUserId,
                LessonReportResolutionAction.DISMISSED
        );

        return new AdminLessonReportResolutionResultDto(
                lesson.getPublicId(),
                resolvedCount,
                lesson.getLessonModerationStatus(),
                LessonReportResolutionAction.DISMISSED.name()
        );
    }

    @Transactional
    public AdminLessonReportResolutionResultDto unpublishAndResolvePendingReports(UUID lessonPublicId, SupabaseAuthUser user) {
        UUID actorUserId = requireActorUserId(user);
        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        Lesson saved = lessonModerationWorkflowService.unpublish(lesson);
        int resolvedCount = resolvePendingReports(
                saved.getLessonId(),
                actorUserId,
                LessonReportResolutionAction.UNPUBLISHED
        );

        return new AdminLessonReportResolutionResultDto(
                saved.getPublicId(),
                resolvedCount,
                saved.getLessonModerationStatus(),
                LessonReportResolutionAction.UNPUBLISHED.name()
        );
    }

    private int resolvePendingReports(Integer lessonId, UUID actorUserId, LessonReportResolutionAction action) {
        return lessonReportRepository.resolvePendingForLessonId(
                lessonId,
                LessonReportStatus.PENDING,
                LessonReportStatus.RESOLVED,
                OffsetDateTime.now(),
                actorUserId,
                action
        );
    }

    private UUID requireActorUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated admin user required");
        }
        return user.userId();
    }
}
