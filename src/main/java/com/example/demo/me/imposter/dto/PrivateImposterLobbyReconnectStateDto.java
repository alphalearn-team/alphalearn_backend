package com.example.demo.me.imposter.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateImposterLobbyReconnectStateDto(
        UUID learnerPublicId,
        OffsetDateTime disconnectDeadlineAt
) {
}
