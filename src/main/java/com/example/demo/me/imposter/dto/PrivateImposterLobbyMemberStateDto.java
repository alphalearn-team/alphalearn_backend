package com.example.demo.me.imposter.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrivateImposterLobbyMemberStateDto(
        UUID learnerPublicId,
        String username,
        OffsetDateTime joinedAt,
        boolean host
) {
}
