package com.example.demo.contributor.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DemoteContributorsRequest", description = "Admin request to demote contributors")
public record DemoteContributorsRequest(
        @Schema(description = "Contributor public UUIDs to demote")
        List<UUID> contributorPublicIds
) {}
