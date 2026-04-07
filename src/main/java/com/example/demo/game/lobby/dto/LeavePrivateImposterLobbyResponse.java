package com.example.demo.game.lobby.dto;

public record LeavePrivateImposterLobbyResponse(
        PrivateImposterLobbyLeaveResult result,
        PrivateImposterLobbyStateDto lobbyState
) {
}
