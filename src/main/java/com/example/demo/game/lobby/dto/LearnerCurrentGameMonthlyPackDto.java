package com.example.demo.game.lobby.dto;

import java.util.List;

public record LearnerCurrentGameMonthlyPackDto(
        String yearMonth,
        boolean exists,
        short currentWeekSlot,
        List<LearnerGameMonthlyPackVisibleConceptDto> visibleConcepts,
        List<LearnerGameMonthlyPackWeeklyFeaturedSlotDto> weeklyFeaturedSlots
) {
}
