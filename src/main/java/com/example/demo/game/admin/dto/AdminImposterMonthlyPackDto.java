package com.example.demo.game.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminImposterMonthlyPackDto(
        String yearMonth,
        boolean exists,
        List<AdminImposterMonthlyPackConceptDto> concepts,
        List<UUID> weeklyFeaturedConceptPublicIds
) {
}
