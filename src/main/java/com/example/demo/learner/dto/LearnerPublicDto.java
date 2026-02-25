package com.example.demo.learner.dto;

import java.util.UUID;

public record LearnerPublicDto(
        UUID publicId,
        String username
) {}
