package com.example.demo.me.imposter.dto;

import java.util.UUID;

public record PrivateImposterLobbyVoteTallyDto(
        UUID learnerPublicId,
        int voteCount
) {
}
