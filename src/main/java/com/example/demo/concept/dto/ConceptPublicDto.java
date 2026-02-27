package com.example.demo.concept.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConceptPublic", description = "Public concept payload")
public record ConceptPublicDto(
        @Schema(description = "Stable public UUID for the concept")
        UUID publicId,
        @Schema(description = "Concept title")
        String title,
        @Schema(description = "Concept description")
        String description,
        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt
) {}
