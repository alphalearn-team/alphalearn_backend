package com.example.demo.learner.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LearnerPublic", description = "Public learner profile payload")
public record LearnerPublicDto(
        @Schema(description = "Learner public UUID")
        UUID publicId,
        @Schema(description = "Learner username")
        String username
) {}
