package com.example.demo.contributor.application.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "AdminContributorApplicationModerationActionRequest",
        description = "Payload for contributor application moderation actions"
)
public record AdminContributorApplicationModerationActionRequest(
        @Schema(description = "Supported values: APPROVE, REJECT")
        String action,
        @Schema(description = "Required when action is REJECT")
        String reason
) {}
