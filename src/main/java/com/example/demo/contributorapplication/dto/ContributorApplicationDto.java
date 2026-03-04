package com.example.demo.contributorapplication.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ContributorApplication", description = "Learner contributor application payload")
public record ContributorApplicationDto(
        @Schema(description = "Stable public UUID for the contributor application", example = "6ac0c4e9-0648-46a0-97ee-c9b6f7f938d6")
        UUID publicId,
        @Schema(description = "Workflow status", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
        String status,
        @Schema(description = "Submission timestamp", example = "2026-03-05T12:00:00Z")
        OffsetDateTime submittedAt,
        @Schema(description = "Review timestamp if the application has been reviewed", example = "2026-03-06T08:00:00Z")
        OffsetDateTime reviewedAt,
        @Schema(description = "Reason for rejection, if rejected", example = "Please build more lessons first.")
        String rejectionReason
) {}
