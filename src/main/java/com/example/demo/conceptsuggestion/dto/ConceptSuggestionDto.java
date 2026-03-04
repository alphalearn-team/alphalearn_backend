package com.example.demo.conceptsuggestion.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConceptSuggestion", description = "Draft or reviewed concept suggestion payload")
public record ConceptSuggestionDto(
        @Schema(description = "Stable public UUID for the concept suggestion")
        UUID publicId,
        @Schema(description = "Draft title")
        String title,
        @Schema(description = "Draft description")
        String description,
        @Schema(description = "Workflow status")
        String status,
        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt,
        @Schema(description = "Last update timestamp")
        OffsetDateTime updatedAt
) {}
