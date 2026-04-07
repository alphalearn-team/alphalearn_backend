package com.example.demo.game.admin.dto;

import java.util.List;
import java.util.UUID;

public record UpsertAdminImposterMonthlyPackRequest(
        List<UUID> conceptPublicIds,
        List<UUID> weeklyFeaturedConceptPublicIds
) {
}
