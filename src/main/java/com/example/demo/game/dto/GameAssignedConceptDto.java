package com.example.demo.game.dto;

import java.util.UUID;

public record GameAssignedConceptDto(
        UUID conceptPublicId,
        String word
) {
}
