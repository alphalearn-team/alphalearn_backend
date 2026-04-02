package com.example.demo.me.imposter.dto;

import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;

public record CreatePrivateImposterLobbyRequest(
        ImposterLobbyConceptPoolMode conceptPoolMode
) {
}
