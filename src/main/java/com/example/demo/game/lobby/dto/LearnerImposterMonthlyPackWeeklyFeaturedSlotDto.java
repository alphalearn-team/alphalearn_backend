package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record LearnerImposterMonthlyPackWeeklyFeaturedSlotDto(
        short weekSlot,
        boolean revealed,
        UUID conceptPublicId,
        String title
) {
}
