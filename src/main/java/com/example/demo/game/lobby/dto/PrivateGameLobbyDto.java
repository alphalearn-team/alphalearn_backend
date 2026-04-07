package com.example.demo.game.lobby.dto;

import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateGameLobbyDto(
        UUID publicId,
        String lobbyCode,
        boolean isPrivate,
        GameLobbyConceptPoolMode conceptPoolMode,
        String pinnedYearMonth,
        OffsetDateTime createdAt
) {
    public static PrivateGameLobbyDto from(GameLobby lobby) {
        return new PrivateGameLobbyDto(
                lobby.getPublicId(),
                lobby.getLobbyCode(),
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getCreatedAt()
        );
    }
}
