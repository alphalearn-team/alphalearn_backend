package com.example.demo.game.imposter.dto;

import java.util.List;
import java.util.UUID;

public record NextImposterConceptRequest(
        List<UUID> excludedConceptPublicIds
) {
}
