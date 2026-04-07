package com.example.demo.admin.lessonreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.admin.lesson.AdminLessonReviewDto;
import com.example.demo.admin.lesson.AdminLessonService;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lessonreport.LessonReport;
import com.example.demo.lessonreport.LessonReportRepository;
import com.example.demo.lessonreport.LessonReportStatus;

@ExtendWith(MockitoExtension.class)
class AdminLessonReportServiceTest {

    @Mock
    private LessonReportRepository lessonReportRepository;
    @Mock
    private AdminLessonService adminLessonService;
    @Mock
    private LessonLookupService lessonLookupService;

    private AdminLessonReportService adminLessonReportService;

    @BeforeEach
    void setUp() {
        adminLessonReportService = new AdminLessonReportService(
                lessonReportRepository,
                adminLessonService,
                lessonLookupService
        );
    }

    @Test
    void listPendingReportedLessonsReturnsMappedSummaryRows() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID authorPublicId = UUID.randomUUID();
        java.time.Instant latestReportedAt = java.time.Instant.parse("2026-04-06T12:00:00Z");

        LessonReportRepository.PendingLessonAggregateProjection projection = new LessonReportRepository.PendingLessonAggregateProjection() {
            public UUID getLessonPublicId() { return lessonPublicId; }
            public String getTitle() { return "Reported Lesson"; }
            public UUID getAuthorPublicId() { return authorPublicId; }
            public String getAuthorUsername() { return "author-1"; }
            public String getLessonModerationStatus() { return "APPROVED"; }
            public long getPendingReportCount() { return 3; }
            public String getLatestReason() { return "Contains incorrect information"; }
            public java.time.Instant getLatestReportedAt() { return latestReportedAt; }
        };

        when(lessonReportRepository.findPendingLessonAggregates()).thenReturn(List.of(projection));
        List<AdminReportedLessonSummaryDto> result = adminLessonReportService.listPendingReportedLessons();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lessonPublicId()).isEqualTo(lessonPublicId);
        assertThat(result.get(0).pendingReportCount()).isEqualTo(3);
        assertThat(result.get(0).latestReportedAt()).isEqualTo(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
    }

    @Test
    void getPendingReportedLessonDetailReturnsLessonAndPendingReasons() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        AdminLessonReviewDto reviewDto = new AdminLessonReviewDto(
                lessonPublicId, "Reported Lesson", java.util.Map.of("body", "content"), List.of(), null,
                LessonModerationStatus.APPROVED, List.of(), null, OffsetDateTime.parse("2026-04-06T10:00:00Z"),
                null, List.of(), 0
        );
        LessonReport report = new LessonReport();
        report.setPublicId(reportId);
        report.setReason("First reason");
        report.setCreatedAt(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
        report.setStatus(LessonReportStatus.PENDING);

        when(adminLessonService.getLessonByPublicId(lessonPublicId)).thenReturn(reviewDto);
        when(lessonReportRepository.findByLesson_PublicIdAndStatusOrderByCreatedAtDesc(lessonPublicId, LessonReportStatus.PENDING))
                .thenReturn(List.of(report));

        AdminReportedLessonDetailDto result = adminLessonReportService.getPendingReportedLessonDetail(lessonPublicId);
        assertThat(result.lesson()).isEqualTo(reviewDto);
        assertThat(result.pendingReportCount()).isEqualTo(1);
    }

    @Test
    void dismissPendingReportDismissesSinglePendingReport() {
        UUID reportPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        when(lessonReportRepository.resolvePendingByReportPublicId(
                eq(reportPublicId), any(), any(), any(), eq(adminUserId), any()
        )).thenReturn(1);

        adminLessonReportService.dismissPendingReport(reportPublicId, new SupabaseAuthUser(adminUserId, null, null));
    }

    @Test
    void dismissPendingReportThrowsNotFoundWhenNoPendingReport() {
        UUID reportPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        when(lessonReportRepository.resolvePendingByReportPublicId(
                eq(reportPublicId), any(), any(), any(), eq(adminUserId), any()
        )).thenReturn(0);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> adminLessonReportService.dismissPendingReport(reportPublicId, new SupabaseAuthUser(adminUserId, null, null))
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void dismissPendingReportsForLessonDismissesAllPendingReports() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        Lesson lesson = lesson(42, lessonPublicId);
        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);

        adminLessonReportService.dismissPendingReportsForLesson(lessonPublicId, new SupabaseAuthUser(adminUserId, null, null));
    }

    private Lesson lesson(int lessonId, UUID lessonPublicId) {
        UUID contributorId = UUID.randomUUID();
        Learner learner = new Learner(contributorId, UUID.randomUUID(), "u-" + contributorId, OffsetDateTime.now(), (short) 0);
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.now());

        Lesson lesson = new Lesson();
        lesson.setPublicId(lessonPublicId);
        lesson.setTitle("Lesson");
        lesson.setContributor(contributor);
        lesson.setCreatedAt(OffsetDateTime.now());
        lesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
        ReflectionTestUtils.setField(lesson, "lessonId", lessonId);
        return lesson;
    }
}
