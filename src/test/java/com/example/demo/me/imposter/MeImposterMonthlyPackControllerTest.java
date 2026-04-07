package com.example.demo.me.imposter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.me.imposter.dto.LearnerCurrentImposterMonthlyPackDto;
import com.example.demo.me.imposter.dto.LearnerImposterMonthlyPackVisibleConceptDto;
import com.example.demo.me.imposter.dto.LearnerImposterMonthlyPackWeeklyFeaturedSlotDto;
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
class MeImposterMonthlyPackControllerTest {

    @Mock
    private LearnerImposterMonthlyPackService learnerImposterMonthlyPackService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new MeImposterMonthlyPackController(learnerImposterMonthlyPackService))
                .build();
    }

    @Test
    void getCurrentMonthlyPackReturnsLearnerSafePayload() throws Exception {
        UUID visibleConceptId = UUID.randomUUID();
        UUID revealedFeaturedConceptId = UUID.randomUUID();
        when(learnerImposterMonthlyPackService.getCurrentMonthlyPack(any()))
                .thenReturn(new LearnerCurrentImposterMonthlyPackDto(
                        "2026-04",
                        true,
                        (short) 2,
                        List.of(
                                new LearnerImposterMonthlyPackVisibleConceptDto((short) 1, visibleConceptId, "alpha", false, null),
                                new LearnerImposterMonthlyPackVisibleConceptDto((short) 2, revealedFeaturedConceptId, "beta", true, (short) 1)
                        ),
                        List.of(
                                new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto((short) 1, true, revealedFeaturedConceptId, "beta"),
                                new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto((short) 2, true, UUID.randomUUID(), "gamma"),
                                new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto((short) 3, false, null, null),
                                new LearnerImposterMonthlyPackWeeklyFeaturedSlotDto((short) 4, false, null, null)
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
