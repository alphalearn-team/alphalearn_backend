package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record PrivateGameLobbyViewerStateDto(
        UUID viewerVoteTargetPublicId,
        boolean viewerIsGame,
        String viewerConceptTitle
) {
}
