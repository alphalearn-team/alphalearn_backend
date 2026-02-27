package com.example.demo.contributor.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ContributorPublic", description = "Public contributor status payload")
public record ContributorPublicDto(
        @Schema(description = "Learner public UUID for this contributor")
        UUID publicId,
        @Schema(description = "Learner username")
        String username,
        @Schema(description = "Timestamp when contributor was promoted")
        OffsetDateTime promotedAt,
        @Schema(description = "Timestamp when contributor was demoted; null means still active")
        OffsetDateTime demotedAt
) {}
