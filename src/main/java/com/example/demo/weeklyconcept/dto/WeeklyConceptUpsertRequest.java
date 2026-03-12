package com.example.demo.weeklyconcept.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WeeklyConceptUpsertRequest", description = "Request payload to set or update weekly concept")
public record WeeklyConceptUpsertRequest(
        @Schema(description = "Concept text for the selected week", example = "Linear equations and inequalities")
        String concept
) {}
