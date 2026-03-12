package com.example.demo.weeklyconcept.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WeeklyConcept", description = "Weekly concept payload for admin management")
public record WeeklyConceptResponse(
        @Schema(description = "Week start date used as key", example = "2026-03-09")
        LocalDate weekStartDate,
        @Schema(description = "Public UUID of the selected concept")
        UUID conceptPublicId,
        @Schema(description = "Title of the selected concept")
        String conceptTitle,
        @Schema(description = "Last update timestamp")
        OffsetDateTime updatedAt
) {}
