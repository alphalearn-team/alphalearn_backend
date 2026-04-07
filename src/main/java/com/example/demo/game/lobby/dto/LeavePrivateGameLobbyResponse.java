package com.example.demo.game.lobby.dto;

public record LeavePrivateGameLobbyResponse(
        PrivateGameLobbyLeaveResult result,
        PrivateGameLobbyStateDto lobbyState
) {
}
