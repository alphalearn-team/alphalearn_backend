package com.example.demo.contributor.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PromoteContributorsRequest", description = "Admin request to promote learners to contributors")
public record PromoteContributorsRequest(
        @Schema(description = "Learner public UUIDs to promote")
        List<UUID> learnerPublicIds
) {}
