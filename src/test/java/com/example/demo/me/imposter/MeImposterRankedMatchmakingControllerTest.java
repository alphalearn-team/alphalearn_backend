package com.example.demo.me.imposter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.me.imposter.dto.RankedImposterMatchmakingStatusDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MeImposterRankedMatchmakingControllerTest {

    @Mock
    private LearnerImposterRankedMatchmakingService learnerImposterRankedMatchmakingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeImposterRankedMatchmakingController(
                learnerImposterRankedMatchmakingService
        )).build();
    }

    @Test
    void enqueueReturnsQueuedStatus() throws Exception {
        when(learnerImposterRankedMatchmakingService.enqueue(any()))
                .thenReturn(new RankedImposterMatchmakingStatusDto(
                        "QUEUED",
                        null,
                        OffsetDateTime.parse("2026-04-05T00:00:00Z"),
                        null,
                        null
                ));

        mockMvc.perform(post("/api/me/imposter/matchmaking/ranked/enqueue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.lobbyPublicId").doesNotExist());
    }

    @Test
    void cancelReturnsCancelledStatus() throws Exception {
        when(learnerImposterRankedMatchmakingService.cancel(any()))
                .thenReturn(new RankedImposterMatchmakingStatusDto(
                        "CANCELLED",
                        null,
                        OffsetDateTime.parse("2026-04-05T00:00:00Z"),
                        null,
                        OffsetDateTime.parse("2026-04-05T00:00:05Z")
                ));

        mockMvc.perform(post("/api/me/imposter/matchmaking/ranked/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void statusReturnsMatchedLobby() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        when(learnerImposterRankedMatchmakingService.getStatus(any()))
                .thenReturn(new RankedImposterMatchmakingStatusDto(
                        "MATCHED",
                        lobbyPublicId,
                        OffsetDateTime.parse("2026-04-05T00:00:00Z"),
                        OffsetDateTime.parse("2026-04-05T00:00:10Z"),
                        null
                ));

        mockMvc.perform(get("/api/me/imposter/matchmaking/ranked/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.lobbyPublicId").value(lobbyPublicId.toString()));
    }
}
