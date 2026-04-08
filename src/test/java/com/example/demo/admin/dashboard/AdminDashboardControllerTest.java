package com.example.demo.admin.dashboard;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.admin.dashboard.dto.AdminDashboardAlertDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardDeltaDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardLowPerformingConceptDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardSummaryDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardTopConceptDto;
import com.example.demo.admin.dashboard.dto.AdminDashboardTrendDto;
import com.example.demo.admin.dashboard.enums.AdminDashboardAlertLevel;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(adminDashboardService)).build();
    }

    @Test
    void getSummaryReturnsDashboardMetrics() throws Exception {
        UUID conceptId = UUID.randomUUID();
        when(adminDashboardService.getSummary(null, null, null)).thenReturn(new AdminDashboardSummaryDto(
                44L,
                300L,
                998L,
                11L,
                List.of(new AdminDashboardTopConceptDto(conceptId, "Algebra", 23L))
        ));

        mockMvc.perform(get("/api/admin/dashboard").queryParam("view", "OVERVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonsCreated").value(44))
                .andExpect(jsonPath("$.usersSignedUp").value(300))
                .andExpect(jsonPath("$.lessonsEnrolled").value(998))
                .andExpect(jsonPath("$.newContributors").value(11))
                .andExpect(jsonPath("$.topConcepts[0].conceptPublicId").value(conceptId.toString()))
                .andExpect(jsonPath("$.topConcepts[0].title").value("Algebra"))
                .andExpect(jsonPath("$.topConcepts[0].lessonCount").value(23));
    }

            @Test
            void getSummaryWithRangeReturnsOptionalAnalytics() throws Exception {
            UUID topConceptId = UUID.randomUUID();
            UUID lowConceptId = UUID.randomUUID();
            when(adminDashboardService.getSummary(eq("30d"), eq(null), eq(null))).thenReturn(new AdminDashboardSummaryDto(
                44L,
                300L,
                998L,
                11L,
                List.of(new AdminDashboardTopConceptDto(topConceptId, "Algebra", 23L)),
                new AdminDashboardDeltaDto(5, 4, 12, 1),
                List.of(new AdminDashboardTrendDto("2026-03-01", 2, 1, 4, 1)),
                List.of(new AdminDashboardAlertDto("PENDING_MODERATION_WARNING", AdminDashboardAlertLevel.WARNING, "Pending moderation count is building up.")),
                14L,
                List.of(new AdminDashboardLowPerformingConceptDto(lowConceptId, "Geometry", 1L)),
                "30d",
                LocalDate.parse("2026-02-14"),
                LocalDate.parse("2026-03-15"),
                LocalDate.parse("2026-01-15"),
                LocalDate.parse("2026-02-13")
            ));

            mockMvc.perform(get("/api/admin/dashboard").queryParam("view", "OVERVIEW").queryParam("range", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deltas.lessonsCreated").value(5))
                .andExpect(jsonPath("$.appliedRange").value("30d"))
                .andExpect(jsonPath("$.startDate").value("2026-02-14"))
                .andExpect(jsonPath("$.endDate").value("2026-03-15"))
                .andExpect(jsonPath("$.comparisonStartDate").value("2026-01-15"))
                .andExpect(jsonPath("$.comparisonEndDate").value("2026-02-13"))
                .andExpect(jsonPath("$.trends[0].label").value("2026-03-01"))
                .andExpect(jsonPath("$.alerts[0].code").value("PENDING_MODERATION_WARNING"))
                .andExpect(jsonPath("$.pendingModerationCount").value(14))
                .andExpect(jsonPath("$.lowPerformingConcepts[0].conceptPublicId").value(lowConceptId.toString()));
            }

            @Test
            void getSummaryWithCustomDatesPassesDatesToService() throws Exception {
            when(adminDashboardService.getSummary(eq(null), eq(LocalDate.parse("2026-03-01")), eq(LocalDate.parse("2026-03-15"))))
                .thenReturn(new AdminDashboardSummaryDto(
                    1,
                    1,
                    1,
                    1,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    "custom",
                    LocalDate.parse("2026-03-01"),
                    LocalDate.parse("2026-03-15"),
                    LocalDate.parse("2026-02-14"),
                    LocalDate.parse("2026-02-28")
                ));

            mockMvc.perform(get("/api/admin/dashboard")
                    .queryParam("view", "OVERVIEW")
                    .queryParam("startDate", "2026-03-01")
                    .queryParam("endDate", "2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonsCreated").value(1))
                .andExpect(jsonPath("$.appliedRange").value("custom"))
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.endDate").value("2026-03-15"));
            }
}
