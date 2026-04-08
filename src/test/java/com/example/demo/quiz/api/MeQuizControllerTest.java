package com.example.demo.quiz.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
import com.example.demo.quiz.LearnerQuizAttemptService;
import com.example.demo.quiz.dto.QuizAttemptResponse;
import com.example.demo.quiz.dto.SubmitQuizAttemptRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class MeQuizControllerTest {

    @Mock
    private LearnerQuizAttemptService learnerQuizAttemptService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new MeQuizController(learnerQuizAttemptService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitQuizAttemptReturnsAttemptSummary() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new com.example.demo.quiz.dto.QuizQuestionAnswerRequest(UUID.randomUUID(), List.of("option-a"))
        ));
        QuizAttemptResponse response = new QuizAttemptResponse(
                quizPublicId,
                OffsetDateTime.parse("2026-03-16T12:00:00Z"),
                1,
                1,
                true
        );

        when(learnerQuizAttemptService.submitQuizAttempt(eq(quizPublicId), eq(request), eq(learnerUser)))
                .thenReturn(response);

        mockMvc.perform(post("/api/me/quizzes/{quizPublicId}/attempts", quizPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizPublicId").value(quizPublicId.toString()))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.isFirstAttempt").value(true));
    }

    @Test
    void getLatestQuizAttemptReturnsAttemptSummary() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        QuizAttemptResponse response = new QuizAttemptResponse(
                quizPublicId,
                OffsetDateTime.parse("2026-03-16T12:00:00Z"),
                3,
                5,
                false
        );

        when(learnerQuizAttemptService.getLatestQuizAttempt(eq(quizPublicId), eq(learnerUser)))
                .thenReturn(response);

        mockMvc.perform(get("/api/me/quizzes/{quizPublicId}/attempts?view=LATEST", quizPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizPublicId").value(quizPublicId.toString()))
                .andExpect(jsonPath("$.score").value(3))
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andExpect(jsonPath("$.isFirstAttempt").value(false));
    }

    @Test
    void getBestQuizAttemptReturnsAttemptSummary() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        QuizAttemptResponse response = new QuizAttemptResponse(
                quizPublicId,
                OffsetDateTime.parse("2026-03-16T15:00:00Z"),
                5,
                5,
                false
        );

        when(learnerQuizAttemptService.getBestQuizAttempt(eq(quizPublicId), eq(learnerUser)))
                .thenReturn(response);

        mockMvc.perform(get("/api/me/quizzes/{quizPublicId}/attempts?view=BEST", quizPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizPublicId").value(quizPublicId.toString()))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andExpect(jsonPath("$.isFirstAttempt").value(false));
    }

    @Test
    void getLatestQuizAttemptReturnsNotFoundWhenAttemptIsMissing() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        when(learnerQuizAttemptService.getLatestQuizAttempt(eq(quizPublicId), eq(learnerUser)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "No quiz attempt found"));

        mockMvc.perform(get("/api/me/quizzes/{quizPublicId}/attempts?view=LATEST", quizPublicId))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("No quiz attempt found");
                });
    }

    @Test
    void getBestQuizAttemptReturnsNotFoundWhenAttemptIsMissing() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        when(learnerQuizAttemptService.getBestQuizAttempt(eq(quizPublicId), eq(learnerUser)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "No quiz attempt found"));

        mockMvc.perform(get("/api/me/quizzes/{quizPublicId}/attempts?view=BEST", quizPublicId))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("No quiz attempt found");
                });
    }

    @Test
    void submitQuizAttemptReturnsForbiddenWhenCallerIsNotLearner() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser contributorUser = contributorUser();
        setAuthentication(contributorUser, "ROLE_CONTRIBUTOR");

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new com.example.demo.quiz.dto.QuizQuestionAnswerRequest(UUID.randomUUID(), List.of("option-a"))
        ));

        when(learnerQuizAttemptService.submitQuizAttempt(eq(quizPublicId), eq(request), eq(contributorUser)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Learner account required"));

        mockMvc.perform(post("/api/me/quizzes/{quizPublicId}/attempts", quizPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("Learner account required");
                });
    }

    @Test
    void submitQuizAttemptReturnsNotFoundWhenQuizIsMissing() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of(
                new com.example.demo.quiz.dto.QuizQuestionAnswerRequest(UUID.randomUUID(), List.of("option-a"))
        ));

        when(learnerQuizAttemptService.submitQuizAttempt(eq(quizPublicId), eq(request), eq(learnerUser)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Quiz not found"));

        mockMvc.perform(post("/api/me/quizzes/{quizPublicId}/attempts", quizPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("Quiz not found");
                });
    }

    @Test
    void submitQuizAttemptReturnsBadRequestWhenSubmissionIsInvalid() throws Exception {
        UUID quizPublicId = UUID.randomUUID();
        SupabaseAuthUser learnerUser = learnerUser();
        setAuthentication(learnerUser, "ROLE_LEARNER");

        SubmitQuizAttemptRequest request = new SubmitQuizAttemptRequest(List.of());

        when(learnerQuizAttemptService.submitQuizAttempt(eq(quizPublicId), eq(request), eq(learnerUser)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "At least one answer is required"));

        mockMvc.perform(post("/api/me/quizzes/{quizPublicId}/attempts", quizPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("At least one answer is required");
                });
    }

    private void setAuthentication(SupabaseAuthUser user, String authority) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SupabaseAuthenticationToken(
                user,
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject(user.userId() == null ? "" : user.userId().toString())
                        .build(),
                List.of(new SimpleGrantedAuthority(authority))
        ));
        SecurityContextHolder.setContext(context);
    }

    private SupabaseAuthUser learnerUser() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "learner-" + learnerId,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        return new SupabaseAuthUser(learnerId, learner, null);
    }

    private SupabaseAuthUser contributorUser() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "contributor-" + learnerId,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        Contributor contributor = new Contributor();
        contributor.setContributorId(learnerId);
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.parse("2026-03-02T00:00:00Z"));
        return new SupabaseAuthUser(learnerId, null, contributor);
    }
}
