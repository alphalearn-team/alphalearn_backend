package com.example.demo.learner.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LearnerProfile", description = "Public learner profile details payload")
public record LearnerProfileDto(
        @Schema(description = "Learner public UUID")
        UUID publicId,
        @Schema(description = "Learner username")
        String username,
        @Schema(description = "Learner bio")
        String bio,
        @Schema(description = "Learner profile picture URL")
        String profilePictureUrl,
        @Schema(description = "Whether the authenticated learner is friends with this learner")
        boolean viewerIsFriend
) {}
