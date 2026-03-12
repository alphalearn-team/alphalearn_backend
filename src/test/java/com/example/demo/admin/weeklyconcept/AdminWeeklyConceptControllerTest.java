package com.example.demo.admin.weeklyconcept;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.weeklyconcept.dto.WeeklyConceptUpsertRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.weeklyconcept.dto.WeeklyConceptResponse;

@ExtendWith(MockitoExtension.class)
class AdminWeeklyConceptControllerTest {

    @Mock
    private AdminWeeklyConceptService adminWeeklyConceptService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminWeeklyConceptController(adminWeeklyConceptService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getWeeklyConceptReturnsRow() throws Exception {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");

        when(adminWeeklyConceptService.getByWeekStartDate(weekStartDate))
                .thenReturn(new WeeklyConceptResponse(
                        weekStartDate,
                        "Algebra foundations",
                        OffsetDateTime.parse("2026-03-12T10:00:00Z")
                ));

        mockMvc.perform(get("/api/admin/weekly-concepts/{weekStartDate}", weekStartDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStartDate").value("2026-03-09"))
                .andExpect(jsonPath("$.concept").value("Algebra foundations"));
    }

    @Test
    void getWeeklyConceptReturnsNotFoundWhenMissing() throws Exception {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        when(adminWeeklyConceptService.getByWeekStartDate(weekStartDate))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Weekly concept not found for weekStartDate: " + weekStartDate
                ));

        mockMvc.perform(get("/api/admin/weekly-concepts/{weekStartDate}", weekStartDate))
                .andExpect(status().isNotFound());
    }

    @Test
    void upsertWeeklyConceptReturnsOkForCreate() throws Exception {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);

        when(adminWeeklyConceptService.upsertByWeekStartDate(
                eq(weekStartDate),
                eq(new WeeklyConceptUpsertRequest("Algebra foundations")),
                eq(user)
        )).thenReturn(new WeeklyConceptResponse(
                weekStartDate,
                "Algebra foundations",
                OffsetDateTime.parse("2026-03-12T10:00:00Z")
        ));

        mockMvc.perform(put("/api/admin/weekly-concepts/{weekStartDate}", weekStartDate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("concept", "Algebra foundations"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStartDate").value("2026-03-09"))
                .andExpect(jsonPath("$.concept").value("Algebra foundations"));
    }

    @Test
    void upsertWeeklyConceptReturnsOkForUpdate() throws Exception {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);

        when(adminWeeklyConceptService.upsertByWeekStartDate(
                eq(weekStartDate),
                eq(new WeeklyConceptUpsertRequest("Updated focus")),
                eq(user)
        )).thenReturn(new WeeklyConceptResponse(
                weekStartDate,
                "Updated focus",
                OffsetDateTime.parse("2026-03-12T11:00:00Z")
        ));

        mockMvc.perform(put("/api/admin/weekly-concepts/{weekStartDate}", weekStartDate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("concept", "Updated focus"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value("Updated focus"));
    }

    @Test
    void upsertWeeklyConceptReturnsBadRequestForInvalidPayload() throws Exception {
        LocalDate weekStartDate = LocalDate.parse("2026-03-09");
        SupabaseAuthUser user = adminUser();
        setAuthentication(user);

        mockMvc.perform(put("/api/admin/weekly-concepts/{weekStartDate}", weekStartDate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("concept", "   "))))
                .andExpect(status().isBadRequest());
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
