package com.example.demo.admin.lessonreport;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.admin.lesson.AdminLessonService;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.LessonModerationWorkflowService;
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

    @Mock
    private LessonModerationWorkflowService lessonModerationWorkflowService;

    private AdminLessonReportService adminLessonReportService;

    @BeforeEach
    void setUp() {
        adminLessonReportService = new AdminLessonReportService(
                lessonReportRepository,
                adminLessonService,
                lessonLookupService,
                lessonModerationWorkflowService
        );
    }

    @Test
    void listPendingReportedLessonsReturnsMappedSummaryRows() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID authorPublicId = UUID.randomUUID();
        java.time.Instant latestReportedAt = java.time.Instant.parse("2026-04-06T12:00:00Z");

        LessonReportRepository.PendingLessonAggregateProjection projection = new LessonReportRepository.PendingLessonAggregateProjection() {
            @Override
            public UUID getLessonPublicId() {
                return lessonPublicId;
            }

            @Override
            public String getTitle() {
                return "Reported Lesson";
            }

            @Override
            public UUID getAuthorPublicId() {
                return authorPublicId;
            }

            @Override
            public String getAuthorUsername() {
                return "author-1";
            }

            @Override
            public String getLessonModerationStatus() {
                return "APPROVED";
            }

            @Override
            public long getPendingReportCount() {
                return 3;
            }

            @Override
            public String getLatestReason() {
                return "Contains incorrect information";
            }

            @Override
            public java.time.Instant getLatestReportedAt() {
                return latestReportedAt;
            }
        };

        when(lessonReportRepository.findPendingLessonAggregates()).thenReturn(List.of(projection));

        List<AdminReportedLessonSummaryDto> result = adminLessonReportService.listPendingReportedLessons();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lessonPublicId()).isEqualTo(lessonPublicId);
        assertThat(result.get(0).author().publicId()).isEqualTo(authorPublicId);
        assertThat(result.get(0).lessonModerationStatus()).isEqualTo(LessonModerationStatus.APPROVED);
        assertThat(result.get(0).pendingReportCount()).isEqualTo(3);
        assertThat(result.get(0).latestReason()).isEqualTo("Contains incorrect information");
        assertThat(result.get(0).latestReportedAt()).isEqualTo(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
    }

    @Test
    void getPendingReportedLessonDetailReturnsLessonAndPendingReasons() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID report1Id = UUID.randomUUID();
        UUID report2Id = UUID.randomUUID();

        AdminLessonReviewDto reviewDto = new AdminLessonReviewDto(
                lessonPublicId,
                "Reported Lesson",
                java.util.Map.of("body", "Lesson content"),
                List.of(),
                null,
                LessonModerationStatus.APPROVED,
                List.of(),
                null,
                OffsetDateTime.parse("2026-04-06T10:00:00Z"),
                null,
                List.of(),
                0
        );

        LessonReport report1 = new LessonReport();
        report1.setPublicId(report1Id);
        report1.setReason("First reason");
        report1.setCreatedAt(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
        report1.setStatus(LessonReportStatus.PENDING);

        LessonReport report2 = new LessonReport();
        report2.setPublicId(report2Id);
        report2.setReason("Second reason");
        report2.setCreatedAt(OffsetDateTime.parse("2026-04-06T11:00:00Z"));
        report2.setStatus(LessonReportStatus.PENDING);

        when(adminLessonService.getLessonByPublicId(lessonPublicId)).thenReturn(reviewDto);
        when(lessonReportRepository.findByLesson_PublicIdAndStatusOrderByCreatedAtDesc(lessonPublicId, LessonReportStatus.PENDING))
                .thenReturn(List.of(report1, report2));

        AdminReportedLessonDetailDto result = adminLessonReportService.getPendingReportedLessonDetail(lessonPublicId);

        assertThat(result.lesson()).isEqualTo(reviewDto);
        assertThat(result.pendingReportCount()).isEqualTo(2);
        assertThat(result.pendingReports()).extracting(AdminPendingLessonReportReasonDto::reportId)
                .containsExactly(report1Id, report2Id);
        assertThat(result.pendingReports()).extracting(AdminPendingLessonReportReasonDto::reason)
                .containsExactly("First reason", "Second reason");
    }

    @Test
    void dismissPendingReportsResolvesAllPendingRows() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        Lesson lesson = lesson(42, lessonPublicId, LessonModerationStatus.APPROVED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonReportRepository.resolvePendingForLessonId(
                org.mockito.ArgumentMatchers.eq(42),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(adminUserId),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(3);

        AdminLessonReportResolutionResultDto result = adminLessonReportService.dismissPendingReports(
                lessonPublicId,
                new SupabaseAuthUser(adminUserId, null, null)
        );

        assertThat(result.lessonPublicId()).isEqualTo(lessonPublicId);
        assertThat(result.resolvedCount()).isEqualTo(3);
        assertThat(result.lessonModerationStatus()).isEqualTo(LessonModerationStatus.APPROVED);
        assertThat(result.action()).isEqualTo("DISMISSED");
    }

    @Test
    void unpublishAndResolvePendingReportsUnpublishesAndResolvesReports() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        Lesson lesson = lesson(88, lessonPublicId, LessonModerationStatus.APPROVED);
        Lesson unpublished = lesson(88, lessonPublicId, LessonModerationStatus.UNPUBLISHED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonModerationWorkflowService.unpublish(lesson)).thenReturn(unpublished);
        when(lessonReportRepository.resolvePendingForLessonId(
                org.mockito.ArgumentMatchers.eq(88),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(adminUserId),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1);

        AdminLessonReportResolutionResultDto result = adminLessonReportService.unpublishAndResolvePendingReports(
                lessonPublicId,
                new SupabaseAuthUser(adminUserId, null, null)
        );

        assertThat(result.resolvedCount()).isEqualTo(1);
        assertThat(result.lessonModerationStatus()).isEqualTo(LessonModerationStatus.UNPUBLISHED);
        assertThat(result.action()).isEqualTo("UNPUBLISHED");
    }

    @Test
    void dismissPendingReportsReturnsZeroWhenNoPendingReportsRemain() {
        UUID lessonPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        Lesson lesson = lesson(52, lessonPublicId, LessonModerationStatus.APPROVED);

        when(lessonLookupService.findByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonReportRepository.resolvePendingForLessonId(
                org.mockito.ArgumentMatchers.eq(52),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(adminUserId),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(0);

        AdminLessonReportResolutionResultDto result = adminLessonReportService.dismissPendingReports(
                lessonPublicId,
                new SupabaseAuthUser(adminUserId, null, null)
        );

        assertThat(result.resolvedCount()).isEqualTo(0);
    }

    @Test
    void dismissPendingReportsRequiresAuthenticatedAdminUser() {
        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> adminLessonReportService.dismissPendingReports(UUID.randomUUID(), null)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Authenticated admin user required");
    }

    private Lesson lesson(int lessonId, UUID lessonPublicId, LessonModerationStatus status) {
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
        lesson.setLessonModerationStatus(status);
        ReflectionTestUtils.setField(lesson, "lessonId", lessonId);
        return lesson;
    }
}
