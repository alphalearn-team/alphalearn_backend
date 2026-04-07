package com.example.demo.game.lobby.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateGameLobbyMemberStateDto(
        UUID learnerPublicId,
        String username,
        OffsetDateTime joinedAt,
        boolean host
) {
}
