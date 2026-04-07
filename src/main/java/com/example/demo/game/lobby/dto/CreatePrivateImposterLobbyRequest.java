package com.example.demo.game.lobby.dto;

import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;

public record CreatePrivateImposterLobbyRequest(
        ImposterLobbyConceptPoolMode conceptPoolMode
) {
}
