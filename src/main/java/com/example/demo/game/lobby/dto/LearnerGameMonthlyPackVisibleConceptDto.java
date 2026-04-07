package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record LearnerGameMonthlyPackVisibleConceptDto(
        short slotIndex,
        UUID conceptPublicId,
        String title,
        boolean weeklyFeatured,
        Short weeklyFeatureWeekSlot
) {
}
