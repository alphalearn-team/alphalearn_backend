package com.example.demo.game.lobby.dto;

import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateImposterLobbyDto(
        UUID publicId,
        String lobbyCode,
        boolean isPrivate,
        ImposterLobbyConceptPoolMode conceptPoolMode,
        String pinnedYearMonth,
        OffsetDateTime createdAt
) {
    public static PrivateImposterLobbyDto from(ImposterGameLobby lobby) {
        return new PrivateImposterLobbyDto(
                lobby.getPublicId(),
                lobby.getLobbyCode(),
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getCreatedAt()
        );
    }
}
