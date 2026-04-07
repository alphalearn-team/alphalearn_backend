package com.example.demo.game.lobby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.lobby.dto.LearnerCurrentGameMonthlyPackDto;
import com.example.demo.game.lobby.dto.LearnerGameMonthlyPackVisibleConceptDto;
import com.example.demo.game.lobby.dto.LearnerGameMonthlyPackWeeklyFeaturedSlotDto;
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
class MeGameMonthlyPackControllerTest {

    @Mock
    private LearnerGameMonthlyPackService learnerGameMonthlyPackService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new MeGameMonthlyPackController(learnerGameMonthlyPackService))
                .build();
    }

    @Test
    void getCurrentMonthlyPackReturnsLearnerSafePayload() throws Exception {
        UUID visibleConceptId = UUID.randomUUID();
        UUID revealedFeaturedConceptId = UUID.randomUUID();
        when(learnerGameMonthlyPackService.getCurrentMonthlyPack(any()))
                .thenReturn(new LearnerCurrentGameMonthlyPackDto(
                        "2026-04",
                        true,
                        (short) 2,
                        List.of(
                                new LearnerGameMonthlyPackVisibleConceptDto((short) 1, visibleConceptId, "alpha", false, null),
                                new LearnerGameMonthlyPackVisibleConceptDto((short) 2, revealedFeaturedConceptId, "beta", true, (short) 1)
                        ),
                        List.of(
                                new LearnerGameMonthlyPackWeeklyFeaturedSlotDto((short) 1, true, revealedFeaturedConceptId, "beta"),
                                new LearnerGameMonthlyPackWeeklyFeaturedSlotDto((short) 2, true, UUID.randomUUID(), "gamma"),
                                new LearnerGameMonthlyPackWeeklyFeaturedSlotDto((short) 3, false, null, null),
                                new LearnerGameMonthlyPackWeeklyFeaturedSlotDto((short) 4, false, null, null)
                        )
                ));

        mockMvc.perform(get("/api/me/imposter/monthly-pack/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes("{}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-04"))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.currentWeekSlot").value(2))
                .andExpect(jsonPath("$.visibleConcepts[0].conceptPublicId").value(visibleConceptId.toString()))
                .andExpect(jsonPath("$.visibleConcepts[1].weeklyFeatured").value(true))
                .andExpect(jsonPath("$.weeklyFeaturedSlots[2].revealed").value(false))
                .andExpect(jsonPath("$.weeklyFeaturedSlots[2].conceptPublicId").doesNotExist());
    }
}
