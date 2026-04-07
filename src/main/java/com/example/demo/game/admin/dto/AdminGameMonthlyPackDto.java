package com.example.demo.game.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminGameMonthlyPackDto(
        String yearMonth,
        boolean exists,
        List<AdminGameMonthlyPackConceptDto> concepts,
        List<UUID> weeklyFeaturedConceptPublicIds
) {
}
