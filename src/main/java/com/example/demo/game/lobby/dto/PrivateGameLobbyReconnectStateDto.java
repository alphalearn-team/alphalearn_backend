package com.example.demo.game.lobby.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateGameLobbyReconnectStateDto(
        UUID learnerPublicId,
        OffsetDateTime disconnectDeadlineAt
) {
}
