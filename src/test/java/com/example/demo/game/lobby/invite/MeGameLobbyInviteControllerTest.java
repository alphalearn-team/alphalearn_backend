package com.example.demo.game.lobby.invite;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.lobby.invite.dto.GameLobbyInviteDto;
import com.example.demo.game.lobby.invite.dto.UpdatePrivateGameLobbyInviteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
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
class MeGameLobbyInviteControllerTest {

    @Mock
    private GameLobbyInviteService gameLobbyInviteService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new MeGameLobbyInviteController(gameLobbyInviteService))
                .build();
    }

    @Test
    void inviteFriendsReturnsCreatedInvites() throws Exception {
        UUID invitePublicId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();

        when(gameLobbyInviteService.inviteFriends(any(), eq(lobbyPublicId), any()))
                .thenReturn(List.of(dto(invitePublicId, lobbyPublicId, GameLobbyInviteStatus.PENDING)));

        mockMvc.perform(post("/api/me/game-lobbies/private-lobbies/{lobbyPublicId}/invites", lobbyPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].invitePublicId").value(invitePublicId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void listInvitesReturnsIncomingPendingInvites() throws Exception {
        UUID invitePublicId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();

        when(gameLobbyInviteService.listInvites(any(), eq(GameLobbyInviteDirection.INCOMING), eq(GameLobbyInviteStatus.PENDING)))
                .thenReturn(List.of(dto(invitePublicId, lobbyPublicId, GameLobbyInviteStatus.PENDING)));

        mockMvc.perform(get("/api/me/game-lobbies/private-invites")
                        .queryParam("direction", "incoming")
                        .queryParam("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invitePublicId").value(invitePublicId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void listInvitesReturnsBadRequestForInvalidDirection() throws Exception {
        mockMvc.perform(get("/api/me/game-lobbies/private-invites")
                        .queryParam("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void respondToInviteReturnsUpdatedInvite() throws Exception {
        UUID invitePublicId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();
        UpdatePrivateGameLobbyInviteRequest request = new UpdatePrivateGameLobbyInviteRequest("ACCEPT");

        when(gameLobbyInviteService.respondToInvite(any(), eq(invitePublicId), eq("ACCEPT")))
                .thenReturn(dto(invitePublicId, lobbyPublicId, GameLobbyInviteStatus.ACCEPTED));

        mockMvc.perform(patch("/api/me/game-lobbies/private-invites/{invitePublicId}", invitePublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitePublicId").value(invitePublicId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void cancelInviteReturnsNoContent() throws Exception {
        UUID invitePublicId = UUID.randomUUID();
        doNothing().when(gameLobbyInviteService).cancelInvite(any(), eq(invitePublicId));

        mockMvc.perform(delete("/api/me/game-lobbies/private-invites/{invitePublicId}", invitePublicId))
                .andExpect(status().isNoContent());
    }

    private GameLobbyInviteDto dto(UUID invitePublicId, UUID lobbyPublicId, GameLobbyInviteStatus status) {
        return new GameLobbyInviteDto(
                invitePublicId,
                lobbyPublicId,
                "ABCD2345",
                UUID.randomUUID(),
                "sender",
                UUID.randomUUID(),
                "receiver",
                status,
                OffsetDateTime.parse("2026-04-09T00:00:00Z"),
                null,
                null
        );
    }
}
