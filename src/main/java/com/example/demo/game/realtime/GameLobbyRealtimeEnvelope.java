package com.example.demo.game.realtime;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GameLobbyRealtimeEnvelope(
        String type,
        UUID lobbyPublicId,
        Integer stateVersion,
        String reason,
        OffsetDateTime emittedAt,
        Object state
) {
}
