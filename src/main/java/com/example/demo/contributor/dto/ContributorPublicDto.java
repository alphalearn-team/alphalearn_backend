package com.example.demo.contributor.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContributorPublicDto(
        UUID publicId,
        String username,
        OffsetDateTime promotedAt,
        OffsetDateTime demotedAt
) {}
