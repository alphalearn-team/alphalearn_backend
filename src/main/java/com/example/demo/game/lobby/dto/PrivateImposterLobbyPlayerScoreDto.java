package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record PrivateImposterLobbyPlayerScoreDto(
        UUID learnerPublicId,
        int points
) {
}
