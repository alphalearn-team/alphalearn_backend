package com.example.demo.game.lobby.dto;

import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JoinedPrivateGameLobbyDto(
        UUID publicId,
        String lobbyCode,
        boolean isPrivate,
        GameLobbyConceptPoolMode conceptPoolMode,
        String pinnedYearMonth,
        OffsetDateTime createdAt,
        OffsetDateTime joinedAt,
        boolean alreadyMember
) {

    public static JoinedPrivateGameLobbyDto from(
            GameLobby lobby,
            GameLobbyMember member,
            boolean alreadyMember
    ) {
        return new JoinedPrivateGameLobbyDto(
                lobby.getPublicId(),
                lobby.getLobbyCode(),
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getCreatedAt(),
                member.getJoinedAt(),
                alreadyMember
        );
    }
}
