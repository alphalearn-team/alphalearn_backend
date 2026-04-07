package com.example.demo.game.dto;

import java.util.List;
import java.util.UUID;

public record NextGameConceptRequest(
        List<UUID> excludedConceptPublicIds,
        UUID lobbyPublicId,
        String lobbyCode
) {
    public NextGameConceptRequest(List<UUID> excludedConceptPublicIds) {
        this(excludedConceptPublicIds, null, null);
    }

    public NextGameConceptRequest(List<UUID> excludedConceptPublicIds, UUID lobbyPublicId) {
        this(excludedConceptPublicIds, lobbyPublicId, null);
    }
}
