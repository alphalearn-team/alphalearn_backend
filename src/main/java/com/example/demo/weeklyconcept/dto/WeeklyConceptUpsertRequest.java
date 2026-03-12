package com.example.demo.weeklyconcept.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WeeklyConceptUpsertRequest", description = "Request payload to set or update weekly concept")
public record WeeklyConceptUpsertRequest(
        @NotNull(message = "conceptPublicId is required")
        @Schema(
                description = "Public UUID of an existing concept",
                example = "123e4567-e89b-12d3-a456-426614174000"
        )
        UUID conceptPublicId
) {}
