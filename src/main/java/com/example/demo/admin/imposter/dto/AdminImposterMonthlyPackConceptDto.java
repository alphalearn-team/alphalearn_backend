package com.example.demo.admin.imposter.dto;

import java.util.UUID;

public record AdminImposterMonthlyPackConceptDto(
        short slotIndex,
        UUID conceptPublicId,
        String title
) {
}
