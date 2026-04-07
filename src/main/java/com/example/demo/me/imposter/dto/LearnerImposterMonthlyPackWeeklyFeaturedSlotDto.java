package com.example.demo.me.imposter.dto;

import java.util.UUID;

public record LearnerImposterMonthlyPackWeeklyFeaturedSlotDto(
        short weekSlot,
        boolean revealed,
        UUID conceptPublicId,
        String title
) {
}
