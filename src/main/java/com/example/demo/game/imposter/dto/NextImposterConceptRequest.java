package com.example.demo.game.imposter.dto;

import java.util.List;
import java.util.UUID;

public record NextImposterConceptRequest(
        List<UUID> excludedConceptPublicIds,
        UUID lobbyPublicId
) {
    public NextImposterConceptRequest(List<UUID> excludedConceptPublicIds) {
        this(excludedConceptPublicIds, null);
    }
}
