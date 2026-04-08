package com.example.demo.contributor.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(name = "AdminContributorRoleActionRequest", description = "Payload for contributor role actions")
public record AdminContributorRoleActionRequest(
        @Schema(description = "Supported values: PROMOTE, DEMOTE")
        String action,
        @Schema(description = "Learner public UUIDs for PROMOTE action")
        List<UUID> learnerPublicIds,
        @Schema(description = "Contributor public UUIDs for DEMOTE action")
        List<UUID> contributorPublicIds
) {}
