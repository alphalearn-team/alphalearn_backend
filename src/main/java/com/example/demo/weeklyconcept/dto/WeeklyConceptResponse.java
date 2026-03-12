package com.example.demo.weeklyconcept.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WeeklyConcept", description = "Weekly concept payload for admin management")
public record WeeklyConceptResponse(
        @Schema(description = "Week start date used as key", example = "2026-03-09")
        LocalDate weekStartDate,
        @Schema(description = "Concept text for the week")
        String concept,
        @Schema(description = "Last update timestamp")
        OffsetDateTime updatedAt
) {}
