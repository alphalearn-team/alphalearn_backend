package com.example.demo.admin.conceptsuggestion;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminConceptSuggestionQueueItem", description = "Admin review queue item for a submitted concept suggestion")
public record AdminConceptSuggestionQueueItemDto(
        @Schema(description = "Concept suggestion public UUID")
        UUID publicId,
        @Schema(description = "Suggestion title")
        String title,
        @Schema(description = "Suggestion description")
        String description,
        @Schema(description = "Workflow status")
        String status,
        @Schema(description = "Owner learner public UUID")
        UUID ownerPublicId,
        @Schema(description = "Owner learner username")
        String ownerUsername,
        @Schema(description = "Suggestion creation timestamp")
        OffsetDateTime createdAt,
        @Schema(description = "Submission timestamp")
        OffsetDateTime submittedAt
) {}
