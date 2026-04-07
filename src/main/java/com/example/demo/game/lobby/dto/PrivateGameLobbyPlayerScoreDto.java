package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record PrivateGameLobbyPlayerScoreDto(
        UUID learnerPublicId,
        int points
) {
}
