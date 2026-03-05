package com.example.demo.admin.contributorapplication;

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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.contributorapplication.dto.ContributorApplicationDto;

@ExtendWith(MockitoExtension.class)
class AdminContributorApplicationControllerTest {

    @Mock
    private AdminContributorApplicationService adminContributorApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminContributorApplicationController(adminContributorApplicationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveApplicationReturnsApprovedApplication() throws Exception {
        UUID applicationPublicId = UUID.randomUUID();
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);

        when(adminContributorApplicationService.approveApplication(applicationPublicId, user))
                .thenReturn(new ContributorApplicationDto(
                        applicationPublicId,
                        "APPROVED",
                        OffsetDateTime.parse("2026-03-05T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-05T11:00:00Z"),
                        null
                ));

        mockMvc.perform(put("/api/admin/contributor-applications/{applicationPublicId}/approve", applicationPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(applicationPublicId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getApplicationByPublicIdReturnsApplication() throws Exception {
        UUID applicationPublicId = UUID.randomUUID();
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);

        when(adminContributorApplicationService.getApplicationByPublicId(applicationPublicId, user))
                .thenReturn(new ContributorApplicationDto(
                        applicationPublicId,
                        "PENDING",
                        OffsetDateTime.parse("2026-03-05T10:00:00Z"),
                        null,
                        null
                ));

        mockMvc.perform(get("/api/admin/contributor-applications/{applicationPublicId}", applicationPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(applicationPublicId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectApplicationReturnsRejectedApplication() throws Exception {
        UUID applicationPublicId = UUID.randomUUID();
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);

        when(adminContributorApplicationService.rejectApplication(
                org.mockito.ArgumentMatchers.eq(applicationPublicId),
                org.mockito.ArgumentMatchers.any(RejectContributorApplicationRequest.class),
                org.mockito.ArgumentMatchers.eq(user)
        )).thenReturn(new ContributorApplicationDto(
                applicationPublicId,
                "REJECTED",
                OffsetDateTime.parse("2026-03-05T10:00:00Z"),
                OffsetDateTime.parse("2026-03-05T11:00:00Z"),
                "Need more activity"
        ));

        mockMvc.perform(put("/api/admin/contributor-applications/{applicationPublicId}/reject", applicationPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Need more activity\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(applicationPublicId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Need more activity"));
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
