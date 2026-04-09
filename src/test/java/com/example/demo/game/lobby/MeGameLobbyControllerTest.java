package com.example.demo.game.lobby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import com.example.demo.game.lobby.GameLobbyPhase;
import com.example.demo.game.lobby.dto.CreatePrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.KickPrivateGameLobbyMemberRequest;
import com.example.demo.game.lobby.dto.LeavePrivateGameLobbyResponse;
import com.example.demo.game.lobby.dto.PrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyLeaveResult;
import com.example.demo.game.lobby.dto.PrivateGameLobbyMemberStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import com.example.demo.game.lobby.dto.UpdatePrivateGameLobbySettingsRequest;
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
class MeGameLobbyControllerTest {

    @Mock
    private LearnerGameLobbyService learnerGameLobbyService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new MeGameLobbyController(learnerGameLobbyService))
                .build();
    }

    @Test
    void createPrivateLobbyReturnsCreatedLobby() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        CreatePrivateGameLobbyRequest request = new CreatePrivateGameLobbyRequest(
                GameLobbyConceptPoolMode.CURRENT_MONTH_PACK
        );

        when(learnerGameLobbyService.createPrivateLobby(any(), eq(request)))
                .thenReturn(new PrivateGameLobbyDto(
                        lobbyPublicId,
                        "ABCD2345",
                        true,
                        GameLobbyConceptPoolMode.CURRENT_MONTH_PACK,
                        "2026-04",
                        OffsetDateTime.parse("2026-04-02T00:00:00Z")
                ));

        mockMvc.perform(post("/api/me/game-lobbies/private-lobbies")
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
        JoinPrivateGameLobbyRequest request = new JoinPrivateGameLobbyRequest("ABCD2345");

        when(learnerGameLobbyService.joinPrivateLobby(any(), eq(request)))
                .thenReturn(new JoinedPrivateGameLobbyDto(
                        lobbyPublicId,
                        "ABCD2345",
                        true,
                        GameLobbyConceptPoolMode.FULL_CONCEPT_POOL,
                        null,
                        OffsetDateTime.parse("2026-04-02T00:00:00Z"),
                        OffsetDateTime.parse("2026-04-02T00:00:00Z"),
                        false
                ));

        mockMvc.perform(post("/api/me/game-lobbies/private-memberships")
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
        PrivateGameLobbyStateDto state = stateDto(lobbyPublicId, null, 2, false, false);
        LeavePrivateGameLobbyResponse response = new LeavePrivateGameLobbyResponse(
                PrivateGameLobbyLeaveResult.LEFT_AND_PROMOTED_HOST,
                state
        );

        when(learnerGameLobbyService.leavePrivateLobby(any(), eq(lobbyPublicId))).thenReturn(response);

        mockMvc.perform(patch("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"LEAVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("LEFT_AND_PROMOTED_HOST"))
                .andExpect(jsonPath("$.lobbyState.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.lobbyState.activeMemberCount").value(2))
                .andExpect(jsonPath("$.lobbyState.canLeave").value(false));
    }

    @Test
    void startPrivateLobbyReturnsStartedState() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        PrivateGameLobbyStateDto state = stateDto(
                lobbyPublicId,
                OffsetDateTime.parse("2026-04-02T00:00:00Z"),
                3,
                true,
                false
        );

        when(learnerGameLobbyService.startPrivateLobby(any(), eq(lobbyPublicId))).thenReturn(state);

        mockMvc.perform(patch("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"START\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.startedAt").value("2026-04-02T00:00:00Z"))
                .andExpect(jsonPath("$.canStart").value(false));
    }

    @Test
    void getPrivateLobbyStateReturnsLobbySnapshot() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        PrivateGameLobbyStateDto state = stateDto(lobbyPublicId, null, 3, true, true);

        when(learnerGameLobbyService.getPrivateLobbyState(any(), eq(lobbyPublicId))).thenReturn(state);

        mockMvc.perform(get("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}", lobbyPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.activeMemberCount").value(3))
                .andExpect(jsonPath("$.canStart").value(true));
    }

    @Test
    void updatePrivateLobbySettingsReturnsState() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        UpdatePrivateGameLobbySettingsRequest request = new UpdatePrivateGameLobbySettingsRequest(4, 2, 45, 35, 20);
        PrivateGameLobbyStateDto state = stateDto(lobbyPublicId, null, 3, true, true);

        when(learnerGameLobbyService.updatePrivateLobbySettings(any(), eq(lobbyPublicId), eq(request))).thenReturn(state);

        mockMvc.perform(patch("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}/settings", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()));
    }

    @Test
    void kickPrivateLobbyMemberReturnsUpdatedState() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID memberPublicId = UUID.randomUUID();
        KickPrivateGameLobbyMemberRequest request = new KickPrivateGameLobbyMemberRequest(memberPublicId);
        PrivateGameLobbyStateDto state = stateDto(lobbyPublicId, null, 2, true, false);

        when(learnerGameLobbyService.kickPrivateLobbyMember(any(), eq(lobbyPublicId), eq(memberPublicId))).thenReturn(state);

        mockMvc.perform(patch("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}/members", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(lobbyPublicId.toString()))
                .andExpect(jsonPath("$.activeMemberCount").value(2));
    }

    @Test
    void joinPrivateLobbyReturnsBadRequestWhenServiceRejects() throws Exception {
        JoinPrivateGameLobbyRequest request = new JoinPrivateGameLobbyRequest("  ");

        when(learnerGameLobbyService.joinPrivateLobby(any(), eq(request)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode is required"));

        mockMvc.perform(post("/api/me/game-lobbies/private-memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startPrivateLobbyReturnsConflictWhenServiceRejects() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();

        when(learnerGameLobbyService.startPrivateLobby(any(), eq(lobbyPublicId)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "At least 3 active players are required to start"));

        mockMvc.perform(patch("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"START\"}"))
                .andExpect(status().isConflict());
    }

    private PrivateGameLobbyStateDto stateDto(
            UUID lobbyPublicId,
            OffsetDateTime startedAt,
            long activeCount,
            boolean canLeave,
            boolean canStart
    ) {
        return new PrivateGameLobbyStateDto(
                lobbyPublicId,
                "ABCD2345",
                true,
                GameLobbyConceptPoolMode.FULL_CONCEPT_POOL,
                null,
                3,
                1,
                30,
                30,
                OffsetDateTime.parse("2026-04-01T00:00:00Z"),
                startedAt,
                activeCount,
                List.of(new PrivateGameLobbyMemberStateDto(UUID.randomUUID(), "host", OffsetDateTime.parse("2026-04-01T00:00:00Z"), true)),
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
                GameLobbyPhase.DRAWING,
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
                null,
                false,
                null
        );
    }
}
