package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record PrivateImposterLobbyViewerStateDto(
        UUID viewerVoteTargetPublicId,
        boolean viewerIsImposter,
        String viewerConceptTitle
) {
}
