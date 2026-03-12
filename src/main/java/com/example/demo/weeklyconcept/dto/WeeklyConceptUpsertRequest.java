package com.example.demo.weeklyconcept.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WeeklyConceptUpsertRequest", description = "Request payload to set or update weekly concept")
public record WeeklyConceptUpsertRequest(
        @NotBlank(message = "concept is required")
        @Size(max = 500, message = "concept must be at most 500 characters")
        @Schema(
                description = "Concept text for the selected week",
                example = "Linear equations and inequalities",
                maxLength = 500
        )
        String concept
) {}
