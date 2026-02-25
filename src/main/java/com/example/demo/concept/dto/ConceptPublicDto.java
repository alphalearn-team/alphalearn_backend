package com.example.demo.concept.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConceptPublicDto(
        UUID publicId,
        String title,
        String description,
        OffsetDateTime createdAt
) {}
