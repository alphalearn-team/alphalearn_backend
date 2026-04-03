package com.example.demo.game.imposter.realtime;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImposterLobbyRealtimeEnvelope(
        String type,
        UUID lobbyPublicId,
        Integer stateVersion,
        String reason,
        OffsetDateTime emittedAt,
        Object state
) {
}
