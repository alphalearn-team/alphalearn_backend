package com.example.demo.admin.contributorapplication;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "RejectContributorApplicationRequest",
        description = "Request payload for rejecting a pending contributor application"
)
public record RejectContributorApplicationRequest(
        @Schema(description = "Required non-blank rejection reason", example = "Please complete more learning activities before reapplying.")
        String reason
) {
}
