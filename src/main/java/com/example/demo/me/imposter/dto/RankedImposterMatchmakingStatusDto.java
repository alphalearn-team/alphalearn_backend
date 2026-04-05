package com.example.demo.me.imposter.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RankedImposterMatchmakingStatusDto(
        String status,
        UUID lobbyPublicId,
        OffsetDateTime queuedAt,
        OffsetDateTime matchedAt,
        OffsetDateTime cancelledAt
) {
}
