package com.example.demo.game.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.admin.dto.AdminGameMonthlyPackConceptDto;
import com.example.demo.game.admin.dto.AdminGameMonthlyPackDto;
import com.example.demo.game.admin.dto.UpsertAdminGameMonthlyPackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminGameMonthlyPackControllerTest {

    @Mock
    private AdminGameMonthlyPackService adminGameMonthlyPackService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminGameMonthlyPackController(adminGameMonthlyPackService))
                .build();
    }

    @Test
    void getMonthlyPackReturnsConfiguredMonth() throws Exception {
        UUID conceptPublicId = UUID.randomUUID();
        UUID featuredConceptPublicId = UUID.randomUUID();
        when(adminGameMonthlyPackService.getMonthlyPack("2026-04"))
                .thenReturn(new AdminGameMonthlyPackDto(
                        "2026-04",
                        true,
                        List.of(new AdminGameMonthlyPackConceptDto((short) 1, conceptPublicId, "alpha")),
                        List.of(featuredConceptPublicId)
                ));

        mockMvc.perform(get("/api/admin/imposter/monthly-packs/2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-04"))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.concepts[0].slotIndex").value(1))
                .andExpect(jsonPath("$.concepts[0].conceptPublicId").value(conceptPublicId.toString()))
                .andExpect(jsonPath("$.weeklyFeaturedConceptPublicIds[0]").value(featuredConceptPublicId.toString()));
    }

    @Test
    void getCurrentMonthlyPackReturnsScaffoldWhenNotConfigured() throws Exception {
        when(adminGameMonthlyPackService.getCurrentMonthlyPack())
                .thenReturn(new AdminGameMonthlyPackDto("2026-04", false, List.of(), List.of()));

        mockMvc.perform(get("/api/admin/imposter/monthly-packs/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-04"))
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.weeklyFeaturedConceptPublicIds").isEmpty());
    }

    @Test
    void upsertMonthlyPackReturnsUpdatedPack() throws Exception {
        UUID firstConcept = UUID.randomUUID();
        UUID secondConcept = UUID.randomUUID();
        UUID thirdConcept = UUID.randomUUID();
        UUID fourthConcept = UUID.randomUUID();

        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(
                List.of(
                        firstConcept, secondConcept, thirdConcept, fourthConcept,
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
                ),
                List.of(firstConcept, secondConcept, thirdConcept, fourthConcept)
        );

        when(adminGameMonthlyPackService.upsertMonthlyPack(eq("2026-04"), eq(request)))
                .thenReturn(new AdminGameMonthlyPackDto(
                        "2026-04",
                        true,
                        List.of(new AdminGameMonthlyPackConceptDto((short) 1, firstConcept, "one")),
                        List.of(firstConcept, secondConcept, thirdConcept, fourthConcept)
                ));

        mockMvc.perform(put("/api/admin/imposter/monthly-packs/2026-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.concepts[0].slotIndex").value(1))
                .andExpect(jsonPath("$.weeklyFeaturedConceptPublicIds[3]").value(fourthConcept.toString()));
    }
}
