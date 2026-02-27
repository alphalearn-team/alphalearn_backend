package com.example.demo.me;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserRole", description = "Resolved role for the current authenticated user")
public record UserRoleDto(
        @Schema(description = "Authenticated user UUID from JWT subject")
        UUID userId,
        @Schema(description = "Resolved role: ADMIN, CONTRIBUTOR, LEARNER, or AUTHENTICATED")
        String role
) {}
