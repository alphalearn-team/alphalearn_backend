package com.example.demo.lesson.report.admin;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@ExtendWith(MockitoExtension.class)
class AdminLessonReportResolutionControllerTest {

    @Mock
    private AdminLessonReportService adminLessonReportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLessonReportResolutionController(adminLessonReportService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dismissPendingReportReturnsNoContent() throws Exception {
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);
        UUID reportPublicId = UUID.randomUUID();

        doNothing().when(adminLessonReportService).dismissPendingReport(reportPublicId, user);

        mockMvc.perform(delete("/api/admin/lesson-reports/{reportPublicId}", reportPublicId))
                .andExpect(status().isNoContent());
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
