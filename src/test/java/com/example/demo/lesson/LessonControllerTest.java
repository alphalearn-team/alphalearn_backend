package com.example.demo.lesson;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.dto.LessonDetailDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

    @Mock
    private LessonService lessonService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LessonController(lessonService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitLessonReturnsConflictForPendingLesson() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        SupabaseAuthUser user = contributorUser(ownerId);
        setAuthentication(user);

        when(lessonService.submitLesson(lessonPublicId, user)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Only UNPUBLISHED or REJECTED lessons can be submitted for review."
        ));

        mockMvc.perform(post("/api/lessons/{lessonPublicId}/submit", lessonPublicId))
                .andExpect(status().isConflict())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("Only UNPUBLISHED or REJECTED lessons can be submitted for review.");
                });
    }

    @Test
    void submitLessonReturnsConflictForApprovedLesson() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        SupabaseAuthUser user = contributorUser(ownerId);
        setAuthentication(user);

        when(lessonService.submitLesson(lessonPublicId, user)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Only UNPUBLISHED or REJECTED lessons can be submitted for review."
        ));

        mockMvc.perform(post("/api/lessons/{lessonPublicId}/submit", lessonPublicId))
                .andExpect(status().isConflict())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("Only UNPUBLISHED or REJECTED lessons can be submitted for review.");
                });
    }

    @Test
    void updateLessonReturnsRemoderatedStatusForApprovedLesson() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID lessonPublicId = UUID.randomUUID();
        SupabaseAuthUser user = contributorUser(ownerId);
        setAuthentication(user);

        LessonDetailDto response = new LessonDetailDto(
                lessonPublicId,
                "Approved lesson update",
                java.util.Map.of("body", "approved body"),
                "PENDING",
                List.of(),
                List.of(),
                null,
                OffsetDateTime.parse("2026-03-04T12:00:00Z"),
                List.of("Needs manual review"),
                "AUTO_FLAGGED",
                OffsetDateTime.parse("2026-03-04T12:01:00Z"),
                null,
                List.of(),
                0
        );

        when(lessonService.updateLesson(
                eq(lessonPublicId),
                eq(new com.example.demo.lesson.dto.UpdateLessonRequest(
                        "Approved lesson update",
                        java.util.Map.of("body", "approved body"),
                        null
                )),
                eq(user)
        )).thenReturn(response);

        mockMvc.perform(put("/api/lessons/{lessonPublicId}", lessonPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "title", "Approved lesson update",
                                "content", java.util.Map.of("body", "approved body")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
                .andExpect(jsonPath("$.latestModerationEventType").value("AUTO_FLAGGED"))
                .andExpect(jsonPath("$.latestModerationReasons[0]").value("Needs manual review"));
    }

    private void setAuthentication(SupabaseAuthUser user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SupabaseAuthenticationToken(
                user,
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject(user.userId() == null ? "" : user.userId().toString())
                        .build(),
                List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR"))
        ));
        SecurityContextHolder.setContext(context);
    }

    private SupabaseAuthUser contributorUser(UUID contributorId) {
        Learner learner = new Learner(contributorId, UUID.randomUUID(), "user-" + contributorId, OffsetDateTime.now(), (short) 0);
        Contributor contributor = new Contributor();
        contributor.setContributorId(contributorId);
        contributor.setPromotedAt(OffsetDateTime.now());
        contributor.setLearner(learner);
        return new SupabaseAuthUser(contributorId, learner, contributor);
    }
}
