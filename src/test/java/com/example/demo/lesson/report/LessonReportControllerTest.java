package com.example.demo.lesson.report;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.report.dto.CreateLessonReportRequest;
import com.example.demo.lesson.report.dto.LessonReportResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonReportControllerTest {

    @Mock
    private LessonReportService lessonReportService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LessonReportController(lessonReportService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createLessonReportReturnsCreatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(userId);
        setAuthentication(user);

        when(lessonReportService.createReport(
                eq(new CreateLessonReportRequest(lessonId, "Incorrect content")),
                eq(user)
        )).thenReturn(new LessonReportResponseDto(
                reportId,
                lessonId,
                OffsetDateTime.parse("2026-04-06T10:00:00Z")
        ));

        mockMvc.perform(post("/api/lesson-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "lessonId", lessonId,
                                "reason", "Incorrect content"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()));
    }

    @Test
    void createLessonReportReturnsBadRequestWhenServiceRejectsRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(userId);
        setAuthentication(user);

        when(lessonReportService.createReport(
                eq(new CreateLessonReportRequest(lessonId, " ")),
                eq(user)
        )).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason is required"));

        mockMvc.perform(post("/api/lesson-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "lessonId", lessonId,
                                "reason", " "
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLessonReportReturnsForbiddenWhenServiceRejectsUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(userId);
        setAuthentication(user);

        when(lessonReportService.createReport(
                eq(new CreateLessonReportRequest(lessonId, "Problematic")),
                eq(user)
        )).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner or contributor access required"));

        mockMvc.perform(post("/api/lesson-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "lessonId", lessonId,
                                "reason", "Problematic"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLessonReportReturnsConflictWhenServiceDetectsDuplicate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(userId);
        setAuthentication(user);

        when(lessonReportService.createReport(
                eq(new CreateLessonReportRequest(lessonId, "Duplicate")),
                eq(user)
        )).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this lesson"));

        mockMvc.perform(post("/api/lesson-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "lessonId", lessonId,
                                "reason", "Duplicate"
                        ))))
                .andExpect(status().isConflict());
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

    private SupabaseAuthUser learnerUser(UUID learnerId) {
        Learner learner = new Learner(learnerId, UUID.randomUUID(), "user-" + learnerId, OffsetDateTime.now(), (short) 0);
        return new SupabaseAuthUser(learnerId, learner, null);
    }
}
