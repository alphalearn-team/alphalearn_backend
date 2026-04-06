package com.example.demo.me.imposter.dto;

import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.lobby.ImposterLobbyType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JoinedPrivateImposterLobbyDto(
        UUID publicId,
        String lobbyCode,
        boolean isPrivate,
        ImposterLobbyConceptPoolMode conceptPoolMode,
        String pinnedYearMonth,
        OffsetDateTime createdAt,
        OffsetDateTime joinedAt,
        boolean alreadyMember
) {

    public static JoinedPrivateImposterLobbyDto from(
            ImposterGameLobby lobby,
            ImposterGameLobbyMember member,
            boolean alreadyMember
    ) {
        return new JoinedPrivateImposterLobbyDto(
                lobby.getPublicId(),
                lobby.getLobbyType() == ImposterLobbyType.PRIVATE_CUSTOM ? lobby.getLobbyCode() : null,
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getCreatedAt(),
                member.getJoinedAt(),
                alreadyMember
        );
    }
}
