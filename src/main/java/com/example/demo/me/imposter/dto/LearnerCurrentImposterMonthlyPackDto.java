package com.example.demo.me.imposter.dto;

import java.util.List;

public record LearnerCurrentImposterMonthlyPackDto(
        String yearMonth,
        boolean exists,
        short currentWeekSlot,
        List<LearnerImposterMonthlyPackVisibleConceptDto> visibleConcepts,
        List<LearnerImposterMonthlyPackWeeklyFeaturedSlotDto> weeklyFeaturedSlots
) {
}
