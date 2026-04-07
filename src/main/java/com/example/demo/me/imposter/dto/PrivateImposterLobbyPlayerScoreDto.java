package com.example.demo.me.imposter.dto;

import java.util.UUID;

public record PrivateImposterLobbyPlayerScoreDto(
        UUID learnerPublicId,
        int points
) {
}
