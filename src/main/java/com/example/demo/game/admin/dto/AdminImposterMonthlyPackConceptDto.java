package com.example.demo.game.admin.dto;

import java.util.UUID;

public record AdminImposterMonthlyPackConceptDto(
        short slotIndex,
        UUID conceptPublicId,
        String title
) {
}
