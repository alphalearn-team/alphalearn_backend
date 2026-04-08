package com.example.demo.learner.read;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

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
import com.example.demo.learner.Learner;
import com.example.demo.learner.dto.LearnerProfileDto;
import com.example.demo.learner.dto.LearnerPublicDto;

@ExtendWith(MockitoExtension.class)
class LearnerControllerTest {

    @Mock
    private LearnerQueryService learnerQueryService;

    private MockMvc mockMvc;
    private SupabaseAuthUser authUser;

    @BeforeEach
    void setUp() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "viewer",
                java.time.OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        authUser = new SupabaseAuthUser(learnerId, learner, null);
        mockMvc = MockMvcBuilders.standaloneSetup(new LearnerController(learnerQueryService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        setAuthentication(authUser);
    }

    @Test
    void getLearnersReturnsPublicLearners() throws Exception {
        UUID learnerPublicId = UUID.randomUUID();
        when(learnerQueryService.getAllPublicLearners()).thenReturn(List.of(
                new LearnerPublicDto(
                        learnerPublicId,
                        "learner-user",
                        "https://cdn.example.com/learner-user.png"
                )
        ));

        mockMvc.perform(get("/api/learners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicId").value(learnerPublicId.toString()))
                .andExpect(jsonPath("$[0].username").value("learner-user"))
                .andExpect(jsonPath("$[0].profilePictureUrl").value("https://cdn.example.com/learner-user.png"));
    }

    @Test
    void getLearnerProfileReturnsProfileDetails() throws Exception {
        UUID learnerPublicId = UUID.randomUUID();
        when(learnerQueryService.getLearnerProfile(authUser, learnerPublicId)).thenReturn(new LearnerProfileDto(
                learnerPublicId,
                "friend-user",
                "Friend bio",
                "https://cdn.example.com/friend-user.png",
                true
        ));

        mockMvc.perform(get("/api/learners/{learnerPublicId}", learnerPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(learnerPublicId.toString()))
                .andExpect(jsonPath("$.username").value("friend-user"))
                .andExpect(jsonPath("$.bio").value("Friend bio"))
                .andExpect(jsonPath("$.profilePictureUrl").value("https://cdn.example.com/friend-user.png"))
                .andExpect(jsonPath("$.viewerIsFriend").value(true));
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
