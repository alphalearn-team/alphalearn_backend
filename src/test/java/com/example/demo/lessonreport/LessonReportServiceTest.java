package com.example.demo.lessonreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lessonreport.dto.CreateLessonReportRequest;
import com.example.demo.lessonreport.dto.LessonReportResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonReportServiceTest {

    @Mock
    private LessonReportRepository lessonReportRepository;

    @Mock
    private LessonLookupService lessonLookupService;

    private LessonReportService lessonReportService;

    @BeforeEach
    void setUp() {
        lessonReportService = new LessonReportService(lessonReportRepository, lessonLookupService);
    }

    @Test
    void createReportSavesWhenRequestIsValid() {
        UUID reporterId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lesson(101, lessonPublicId, UUID.randomUUID());

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonReportRepository.existsByLesson_LessonIdAndReporterUserId(101, reporterId)).thenReturn(false);
        when(lessonReportRepository.save(any(LessonReport.class))).thenAnswer(invocation -> {
            LessonReport report = invocation.getArgument(0);
            report.assignDefaultsIfMissing();
            return report;
        });

        LessonReportResponseDto result = lessonReportService.createReport(
                new CreateLessonReportRequest(lessonPublicId, "  Inaccurate content  "),
                learnerUser(reporterId)
        );

        ArgumentCaptor<LessonReport> captor = ArgumentCaptor.forClass(LessonReport.class);
        verify(lessonReportRepository).save(captor.capture());
        LessonReport saved = captor.getValue();
        assertThat(saved.getLesson()).isEqualTo(lesson);
        assertThat(saved.getReporterUserId()).isEqualTo(reporterId);
        assertThat(saved.getReason()).isEqualTo("Inaccurate content");
        assertThat(result.lessonId()).isEqualTo(lessonPublicId);
        assertThat(result.reportId()).isNotNull();
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void createReportRejectsBlankReason() {
        UUID lessonPublicId = UUID.randomUUID();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonReportService.createReport(
                        new CreateLessonReportRequest(lessonPublicId, "   "),
                        learnerUser(UUID.randomUUID())
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("reason is required");
    }

    @Test
    void createReportRejectsSelfReport() {
        UUID userId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lesson(77, lessonPublicId, userId);

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonReportService.createReport(
                        new CreateLessonReportRequest(lessonPublicId, "Bad lesson"),
                        contributorUser(userId)
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Lesson owners cannot report their own lessons");
    }

    @Test
    void createReportRejectsDuplicateReport() {
        UUID userId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        Lesson lesson = lesson(55, lessonPublicId, UUID.randomUUID());

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId)).thenReturn(lesson);
        when(lessonReportRepository.existsByLesson_LessonIdAndReporterUserId(55, userId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonReportService.createReport(
                        new CreateLessonReportRequest(lessonPublicId, "Spam"),
                        learnerUser(userId)
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("You have already reported this lesson");
    }

    @Test
    void createReportRejectsNonLearnerAndNonContributorUser() {
        UUID userId = UUID.randomUUID();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonReportService.createReport(
                        new CreateLessonReportRequest(UUID.randomUUID(), "Issue"),
                        new SupabaseAuthUser(userId, null, null)
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Learner or contributor access required");
    }

    @Test
    void createReportPropagatesLessonNotFound() {
        UUID lessonPublicId = UUID.randomUUID();

        when(lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonReportService.createReport(
                        new CreateLessonReportRequest(lessonPublicId, "Issue"),
                        learnerUser(UUID.randomUUID())
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Lesson not found");
    }

    private Lesson lesson(int lessonId, UUID lessonPublicId, UUID contributorId) {
        Learner learner = new Learner(contributorId, UUID.randomUUID(), "u-" + contributorId, OffsetDateTime.now(), (short) 0);
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.now());

        Lesson lesson = new Lesson(
                "Title",
                new ObjectMapper().valueToTree(java.util.Map.of("body", "content")),
                LessonModerationStatus.APPROVED,
                contributor,
                OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(lesson, "lessonId", lessonId);
        lesson.setPublicId(lessonPublicId);
        return lesson;
    }

    private SupabaseAuthUser learnerUser(UUID learnerId) {
        Learner learner = new Learner(learnerId, UUID.randomUUID(), "learner-" + learnerId, OffsetDateTime.now(), (short) 0);
        return new SupabaseAuthUser(learnerId, learner, null);
    }

    private SupabaseAuthUser contributorUser(UUID contributorId) {
        Learner learner = new Learner(contributorId, UUID.randomUUID(), "contributor-" + contributorId, OffsetDateTime.now(), (short) 0);
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.now());
        return new SupabaseAuthUser(contributorId, learner, contributor);
    }
}
