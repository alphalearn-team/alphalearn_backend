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

import com.example.demo.admin.lesson.AdminLessonReviewDto;
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
        OffsetDateTime latestReportedAt = OffsetDateTime.parse("2026-04-06T12:00:00Z");

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
            public OffsetDateTime getLatestReportedAt() {
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
        assertThat(result.get(0).latestReportedAt()).isEqualTo(latestReportedAt);
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
}
