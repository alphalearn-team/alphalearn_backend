package com.example.demo.me.imposter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MeImposterLobbyControllerTest {

    @Mock
    private LearnerImposterLobbyService learnerImposterLobbyService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new MeImposterLobbyController(learnerImposterLobbyService))
                .build();
    }

    @Test
    void createPrivateLobbyReturnsCreatedLobby() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        CreatePrivateImposterLobbyRequest request = new CreatePrivateImposterLobbyRequest(
                ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK
        );

        when(learnerImposterLobbyService.createPrivateLobby(any(), eq(request)))
                .thenReturn(new PrivateImposterLobbyDto(
                        lobbyPublicId,
                        "ABCD2345",
                        true,
                        ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK,
                        "2026-04",
                        OffsetDateTime.parse("2026-04-02T00:00:00Z")
                ));

        mockMvc.perform(post("/api/me/imposter/lobbies/private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.lobbyCode").value("ABCD2345"))
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.conceptPoolMode").value("CURRENT_MONTH_PACK"))
                .andExpect(jsonPath("$.pinnedYearMonth").value("2026-04"));
    }

    @Test
    void joinPrivateLobbyReturnsJoinedLobby() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        JoinPrivateImposterLobbyRequest request = new JoinPrivateImposterLobbyRequest("ABCD2345");

        when(learnerImposterLobbyService.joinPrivateLobby(any(), eq(request)))
                .thenReturn(new JoinedPrivateImposterLobbyDto(
                        lobbyPublicId,
                        "ABCD2345",
                        true,
                        ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL,
                        null,
                        OffsetDateTime.parse("2026-04-02T00:00:00Z"),
                        OffsetDateTime.parse("2026-04-02T00:00:00Z"),
                        false
                ));

        mockMvc.perform(post("/api/me/imposter/lobbies/private/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.lobbyCode").value("ABCD2345"))
                .andExpect(jsonPath("$.alreadyMember").value(false))
                .andExpect(jsonPath("$.joinedAt").value("2026-04-02T00:00:00Z"));
    }

    @Test
    void joinPrivateLobbyReturnsBadRequestWhenServiceRejects() throws Exception {
        JoinPrivateImposterLobbyRequest request = new JoinPrivateImposterLobbyRequest("  ");

        when(learnerImposterLobbyService.joinPrivateLobby(any(), eq(request)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode is required"));

        mockMvc.perform(post("/api/me/imposter/lobbies/private/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }
}
