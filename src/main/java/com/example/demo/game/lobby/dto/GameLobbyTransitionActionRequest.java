package com.example.demo.game.lobby.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GameLobbyTransitionActionRequest", description = "Payload for private lobby state transition actions")
public record GameLobbyTransitionActionRequest(
        @Schema(description = "Supported values: START, LEAVE")
        String action
) {}
