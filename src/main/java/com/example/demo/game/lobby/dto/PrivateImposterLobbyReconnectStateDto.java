package com.example.demo.game.lobby.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateImposterLobbyReconnectStateDto(
        UUID learnerPublicId,
        OffsetDateTime disconnectDeadlineAt
) {
}
