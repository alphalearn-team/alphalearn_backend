package com.example.demo.me.imposter.dto;

import java.util.UUID;

public record PrivateImposterLobbyViewerStateDto(
        UUID viewerVoteTargetPublicId,
        boolean viewerIsImposter,
        String viewerConceptTitle
) {
}
