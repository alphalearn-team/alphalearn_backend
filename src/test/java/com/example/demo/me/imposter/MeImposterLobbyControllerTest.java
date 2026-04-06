package com.example.demo.me.imposter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.lobby.ImposterLobbyPhase;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.LeavePrivateImposterLobbyResponse;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyLeaveResult;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyMemberStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyStateDto;
import com.example.demo.me.imposter.dto.UpdatePrivateImposterLobbySettingsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

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
    void leavePrivateLobbyReturnsUpdatedState() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        PrivateImposterLobbyStateDto state = stateDto(lobbyPublicId, null, 2, false, false);
        LeavePrivateImposterLobbyResponse response = new LeavePrivateImposterLobbyResponse(
                PrivateImposterLobbyLeaveResult.LEFT_AND_PROMOTED_HOST,
                state
        );

        when(learnerImposterLobbyService.leavePrivateLobby(any(), eq(lobbyPublicId))).thenReturn(response);

        mockMvc.perform(post("/api/me/imposter/lobbies/private/{lobbyPublicId}/leave", lobbyPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("LEFT_AND_PROMOTED_HOST"))
                .andExpect(jsonPath("$.lobbyState.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.lobbyState.activeMemberCount").value(2))
                .andExpect(jsonPath("$.lobbyState.canLeave").value(false));
    }

    @Test
    void leaveCurrentMatchmakingLobbyReturnsUpdatedState() throws Exception {
        LeavePrivateImposterLobbyResponse response = new LeavePrivateImposterLobbyResponse(
                PrivateImposterLobbyLeaveResult.LEFT,
                null
        );

        when(learnerImposterLobbyService.leaveCurrentLobby(any())).thenReturn(response);

        mockMvc.perform(post("/api/me/imposter/lobbies/private/matchmaking/leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("LEFT"));
    }

    @Test
    void startPrivateLobbyReturnsStartedState() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        PrivateImposterLobbyStateDto state = stateDto(
                lobbyPublicId,
                OffsetDateTime.parse("2026-04-02T00:00:00Z"),
                3,
                true,
                false
        );

        when(learnerImposterLobbyService.startPrivateLobby(any(), eq(lobbyPublicId))).thenReturn(state);

        mockMvc.perform(post("/api/me/imposter/lobbies/private/{lobbyPublicId}/start", lobbyPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.startedAt").value("2026-04-02T00:00:00Z"))
                .andExpect(jsonPath("$.canStart").value(false));
    }

    @Test
    void getPrivateLobbyStateReturnsLobbySnapshot() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        PrivateImposterLobbyStateDto state = stateDto(lobbyPublicId, null, 3, true, true);

        when(learnerImposterLobbyService.getPrivateLobbyState(any(), eq(lobbyPublicId))).thenReturn(state);

        mockMvc.perform(get("/api/me/imposter/lobbies/private/{lobbyPublicId}/state", lobbyPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.activeMemberCount").value(3))
                .andExpect(jsonPath("$.canStart").value(true));
    }

    @Test
    void updatePrivateLobbySettingsReturnsState() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        UpdatePrivateImposterLobbySettingsRequest request = new UpdatePrivateImposterLobbySettingsRequest(4, 2, 45, 35, 20);
        PrivateImposterLobbyStateDto state = stateDto(lobbyPublicId, null, 3, true, true);

        when(learnerImposterLobbyService.updatePrivateLobbySettings(any(), eq(lobbyPublicId), eq(request))).thenReturn(state);

        mockMvc.perform(patch("/api/me/imposter/lobbies/private/{lobbyPublicId}/settings", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()));
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

    @Test
    void startPrivateLobbyReturnsConflictWhenServiceRejects() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();

        when(learnerImposterLobbyService.startPrivateLobby(any(), eq(lobbyPublicId)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "At least 3 active players are required to start"));

        mockMvc.perform(post("/api/me/imposter/lobbies/private/{lobbyPublicId}/start", lobbyPublicId))
                .andExpect(status().isConflict());
    }

    private PrivateImposterLobbyStateDto stateDto(
            UUID lobbyPublicId,
            OffsetDateTime startedAt,
            long activeCount,
            boolean canLeave,
            boolean canStart
    ) {
        return new PrivateImposterLobbyStateDto(
                lobbyPublicId,
                "ABCD2345",
                true,
                ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL,
                null,
                3,
                1,
                30,
                30,
                OffsetDateTime.parse("2026-04-01T00:00:00Z"),
                startedAt,
                activeCount,
                List.of(new PrivateImposterLobbyMemberStateDto(UUID.randomUUID(), "host", OffsetDateTime.parse("2026-04-01T00:00:00Z"), true)),
                true,
                true,
                canLeave,
                canStart,
                null,
                null,
                null,
                25,
                null,
                null,
                false,
                false,
                false,
                false,
                null,
                0,
                ImposterLobbyPhase.DRAWING,
                null,
                null,
                null,
                List.of(),
                1,
                3,
                List.of(),
                null,
                3,
                10,
                120,
                0,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );
    }
}
