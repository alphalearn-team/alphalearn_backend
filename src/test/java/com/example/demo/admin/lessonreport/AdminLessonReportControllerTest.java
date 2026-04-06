package com.example.demo.admin.lessonreport;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.admin.lesson.AdminLessonReviewDto;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;

@ExtendWith(MockitoExtension.class)
class AdminLessonReportControllerTest {

    @Mock
    private AdminLessonReportService adminLessonReportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLessonReportController(adminLessonReportService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listPendingReportedLessonsReturnsSummaryRows() throws Exception {
        setAuthentication(adminUser());
        UUID lessonPublicId = UUID.randomUUID();

        when(adminLessonReportService.listPendingReportedLessons()).thenReturn(List.of(
                new AdminReportedLessonSummaryDto(
                        lessonPublicId,
                        "Reported Lesson",
                        new LessonAuthorDto(UUID.randomUUID(), "author"),
                        LessonModerationStatus.APPROVED,
                        2,
                        "Incorrect information",
                        OffsetDateTime.parse("2026-04-06T12:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/admin/lesson-reports/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$[0].pendingReportCount").value(2))
                .andExpect(jsonPath("$[0].latestReason").value("Incorrect information"));
    }

    @Test
    void getPendingReportedLessonDetailReturnsLessonAndReasons() throws Exception {
        setAuthentication(adminUser());
        UUID lessonPublicId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        AdminLessonReviewDto lesson = new AdminLessonReviewDto(
                lessonPublicId,
                "Reported Lesson",
                java.util.Map.of("body", "content"),
                List.of(),
                new LessonAuthorDto(UUID.randomUUID(), "author"),
                LessonModerationStatus.APPROVED,
                List.of(),
                null,
                OffsetDateTime.parse("2026-04-06T10:00:00Z"),
                null,
                List.of(),
                0
        );

        when(adminLessonReportService.getPendingReportedLessonDetail(lessonPublicId)).thenReturn(
                new AdminReportedLessonDetailDto(
                        lesson,
                        List.of(new AdminPendingLessonReportReasonDto(
                                reportId,
                                "Reason text",
                                OffsetDateTime.parse("2026-04-06T12:00:00Z")
                        )),
                        1
                )
        );

        mockMvc.perform(get("/api/admin/lesson-reports/lessons/{lessonPublicId}", lessonPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lesson.lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$.pendingReportCount").value(1))
                .andExpect(jsonPath("$.pendingReports[0].reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.pendingReports[0].reason").value("Reason text"));
    }

    @Test
    void dismissPendingReportsReturnsResolutionResult() throws Exception {
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);
        UUID lessonPublicId = UUID.randomUUID();

        when(adminLessonReportService.dismissPendingReports(lessonPublicId, user)).thenReturn(
                new AdminLessonReportResolutionResultDto(
                        lessonPublicId,
                        2,
                        LessonModerationStatus.APPROVED,
                        "DISMISSED"
                )
        );

        mockMvc.perform(put("/api/admin/lesson-reports/lessons/{lessonPublicId}/dismiss", lessonPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$.resolvedCount").value(2))
                .andExpect(jsonPath("$.action").value("DISMISSED"));
    }

    @Test
    void unpublishAndResolvePendingReportsReturnsResolutionResult() throws Exception {
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);
        UUID lessonPublicId = UUID.randomUUID();

        when(adminLessonReportService.unpublishAndResolvePendingReports(lessonPublicId, user)).thenReturn(
                new AdminLessonReportResolutionResultDto(
                        lessonPublicId,
                        1,
                        LessonModerationStatus.UNPUBLISHED,
                        "UNPUBLISHED"
                )
        );

        mockMvc.perform(put("/api/admin/lesson-reports/lessons/{lessonPublicId}/unpublish", lessonPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$.resolvedCount").value(1))
                .andExpect(jsonPath("$.lessonModerationStatus").value("UNPUBLISHED"))
                .andExpect(jsonPath("$.action").value("UNPUBLISHED"));
    }

    private void setAuthentication(SupabaseAuthUser user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SupabaseAuthenticationToken(
                user,
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject(user.userId().toString())
                        .build(),
                List.of()
        ));
        SecurityContextHolder.setContext(context);
    }

    private SupabaseAuthUser adminUser() {
        return new SupabaseAuthUser(UUID.randomUUID(), null, null);
    }
}
