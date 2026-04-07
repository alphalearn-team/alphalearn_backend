package com.example.demo.game.lobby.dto;

import java.util.List;

public record LearnerCurrentImposterMonthlyPackDto(
        String yearMonth,
        boolean exists,
        short currentWeekSlot,
        List<LearnerImposterMonthlyPackVisibleConceptDto> visibleConcepts,
        List<LearnerImposterMonthlyPackWeeklyFeaturedSlotDto> weeklyFeaturedSlots
) {
}
