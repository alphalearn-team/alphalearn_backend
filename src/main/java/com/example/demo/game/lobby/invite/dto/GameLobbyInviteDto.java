package com.example.demo.game.lobby.invite.dto;

import com.example.demo.game.lobby.invite.GameLobbyInviteStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GameLobbyInviteDto(
        UUID invitePublicId,
        UUID lobbyPublicId,
        String lobbyCode,
        UUID senderPublicId,
        String senderUsername,
        UUID receiverPublicId,
        String receiverUsername,
        GameLobbyInviteStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime respondedAt,
        OffsetDateTime expiresAt
) {
}
