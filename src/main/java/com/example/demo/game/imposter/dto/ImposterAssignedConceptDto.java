package com.example.demo.game.imposter.dto;

import java.util.UUID;

public record ImposterAssignedConceptDto(
        UUID conceptPublicId,
        String word
) {
}
