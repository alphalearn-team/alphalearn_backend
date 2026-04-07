package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record PrivateGameLobbyVoteTallyDto(
        UUID learnerPublicId,
        int voteCount
) {
}
