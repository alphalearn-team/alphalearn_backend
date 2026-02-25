package com.example.demo.admin.me;

import java.util.UUID;

public record UserRoleDto(
        UUID userId,
        String role
) {}
