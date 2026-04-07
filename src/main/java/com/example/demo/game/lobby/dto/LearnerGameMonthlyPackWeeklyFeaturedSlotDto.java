package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record LearnerGameMonthlyPackWeeklyFeaturedSlotDto(
        short weekSlot,
        boolean revealed,
        UUID conceptPublicId,
        String title
) {
}
