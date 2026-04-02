package com.example.demo.me.imposter.dto;

import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PrivateImposterLobbyStateDto(
        UUID publicId,
        String lobbyCode,
        boolean isPrivate,
        ImposterLobbyConceptPoolMode conceptPoolMode,
        String pinnedYearMonth,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        long activeMemberCount,
        List<PrivateImposterLobbyMemberStateDto> activeMembers,
        boolean viewerIsHost,
        boolean viewerIsActiveMember,
        boolean canLeave,
        boolean canStart
) {
}
