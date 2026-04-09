package com.example.demo.game.lobby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "KickPrivateGameLobbyMemberRequest", description = "Payload for host kicking a member from private lobby")
public record KickPrivateGameLobbyMemberRequest(
        @Schema(description = "Target learner public ID to remove from lobby")
        UUID memberPublicId
) {
}
