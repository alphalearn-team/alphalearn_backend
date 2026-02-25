package com.example.demo.admin;

import java.util.UUID;

public record UserRoleDto(
        UUID userId,
        String role
) {}
