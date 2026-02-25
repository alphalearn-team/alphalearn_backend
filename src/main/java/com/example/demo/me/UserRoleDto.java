package com.example.demo.me;

import java.util.UUID;

public record UserRoleDto(
        UUID userId,
        String role
) {}
