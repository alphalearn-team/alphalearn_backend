package com.example.demo.admin.dashboard;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
        when(adminDashboardService.getSummary()).thenReturn(new AdminDashboardSummaryDto(
                44L,
                300L,
                998L,
                11L,
                List.of(new AdminDashboardTopConceptDto(conceptId, "Algebra", 23L))
        ));

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonsCreated").value(44))
                .andExpect(jsonPath("$.usersSignedUp").value(300))
                .andExpect(jsonPath("$.lessonsEnrolled").value(998))
                .andExpect(jsonPath("$.newContributors").value(11))
                .andExpect(jsonPath("$.topConcepts[0].conceptPublicId").value(conceptId.toString()))
                .andExpect(jsonPath("$.topConcepts[0].title").value("Algebra"))
                .andExpect(jsonPath("$.topConcepts[0].lessonCount").value(23));
    }
}
