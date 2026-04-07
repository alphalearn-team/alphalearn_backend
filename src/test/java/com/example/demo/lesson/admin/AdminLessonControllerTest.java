package com.example.demo.lesson.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.dto.LessonAuthorDto;

@ExtendWith(MockitoExtension.class)
class AdminLessonControllerTest {

    @Mock
    private AdminLessonService adminLessonService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLessonController(adminLessonService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateLessonModerationStatusReturnsUpdatedLesson() throws Exception {
        UUID lessonPublicId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        SupabaseAuthUser user = new SupabaseAuthUser(adminUserId, null, null);
        setAuthentication(user);

        when(adminLessonService.updateLessonModerationStatus(
                eq(lessonPublicId),
                eq(new AdminUpdateLessonModerationStatusRequest(LessonModerationStatus.UNPUBLISHED, true)),
                eq(user)
        )).thenReturn(new AdminLessonDetailDto(
                new LessonAuthorDto(UUID.randomUUID(), "author"),
                lessonPublicId,
                "Lesson Title",
                LessonModerationStatus.UNPUBLISHED
        ));

        mockMvc.perform(patch("/api/admin/lessons/{lessonPublicId}/moderation-status", lessonPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNPUBLISHED\",\"resolvePendingReports\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$.lessonModerationStatus").value("UNPUBLISHED"));
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
}
