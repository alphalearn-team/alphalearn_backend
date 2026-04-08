package com.example.demo.contributor.application;

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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.contributor.application.dto.ContributorApplicationDto;
import com.example.demo.learner.Learner;

@ExtendWith(MockitoExtension.class)
class ContributorApplicationControllerTest {

    @Mock
    private ContributorApplicationService contributorApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ContributorApplicationController(contributorApplicationService),
                        new MeContributorApplicationController(contributorApplicationService)
                )
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitApplicationReturnsCreatedApplication() throws Exception {
        UUID learnerId = UUID.randomUUID();
        UUID applicationPublicId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(learnerId);
        setAuthentication(user);

        when(contributorApplicationService.submitApplication(user)).thenReturn(new ContributorApplicationDto(
                applicationPublicId,
                UUID.randomUUID(),
                "learner-one",
                "PENDING",
                OffsetDateTime.parse("2026-03-05T10:00:00Z"),
                null,
                null
        ));

        mockMvc.perform(post("/api/contributor-applications"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").value(applicationPublicId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getMyApplicationsReturnsApplicationHistory() throws Exception {
        UUID learnerId = UUID.randomUUID();
        UUID newestApplicationId = UUID.randomUUID();
        UUID olderApplicationId = UUID.randomUUID();
        SupabaseAuthUser user = learnerUser(learnerId);
        setAuthentication(user);

        when(contributorApplicationService.getMyApplications(user)).thenReturn(List.of(
                new ContributorApplicationDto(
                        newestApplicationId,
                        UUID.randomUUID(),
                        "learner-newest",
                        "PENDING",
                        OffsetDateTime.parse("2026-03-05T10:00:00Z"),
                        null,
                        null
                ),
                new ContributorApplicationDto(
                        olderApplicationId,
                        UUID.randomUUID(),
                        "learner-older",
                        "REJECTED",
                        OffsetDateTime.parse("2026-03-04T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-04T12:00:00Z"),
                        "Please add more activity first."
                )
        ));

        mockMvc.perform(get("/api/me/contributor-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicId").value(newestApplicationId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].publicId").value(olderApplicationId.toString()))
                .andExpect(jsonPath("$[1].status").value("REJECTED"));
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
