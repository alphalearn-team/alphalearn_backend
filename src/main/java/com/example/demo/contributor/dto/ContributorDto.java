package com.example.demo.contributor.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContributorDto(
        UUID contributorId,
        OffsetDateTime promotedAt,
        OffsetDateTime demotedAt
) {}
