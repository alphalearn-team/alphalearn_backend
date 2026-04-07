package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record PrivateImposterLobbyVoteTallyDto(
        UUID learnerPublicId,
        int voteCount
) {
}
