package com.example.demo.conceptsuggestion.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConceptSuggestion", description = "Draft or reviewed concept suggestion payload")
public record ConceptSuggestionDto(
        @Schema(description = "Stable public UUID for the concept suggestion", example = "8d4f56a2-5cd3-4d6c-8706-bbc084d48598")
        UUID publicId,
        @Schema(description = "Draft title", example = "Fractions through pizza sharing")
        String title,
        @Schema(description = "Draft description", example = "An idea for teaching fractions visually through portions and sharing scenarios")
        String description,
        @Schema(description = "Workflow status", example = "DRAFT", allowableValues = {"DRAFT", "SUBMITTED", "APPROVED", "REJECTED"})
        String status,
        @Schema(description = "Creation timestamp", example = "2026-03-04T12:00:00Z")
        OffsetDateTime createdAt,
        @Schema(description = "Last update timestamp", example = "2026-03-04T12:05:00Z")
        OffsetDateTime updatedAt
) {}
