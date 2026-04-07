package com.example.demo.game.lobby.dto;

import com.example.demo.game.lobby.GameLobbyConceptPoolMode;

public record CreatePrivateGameLobbyRequest(
        GameLobbyConceptPoolMode conceptPoolMode
) {
}
