package com.example.demo.me.imposter.dto;

import java.util.UUID;

public record LearnerImposterMonthlyPackVisibleConceptDto(
        short slotIndex,
        UUID conceptPublicId,
        String title,
        boolean weeklyFeatured,
        Short weeklyFeatureWeekSlot
) {
}
