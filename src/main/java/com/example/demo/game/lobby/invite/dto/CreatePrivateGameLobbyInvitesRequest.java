package com.example.demo.game.lobby.invite.dto;

import java.util.List;
import java.util.UUID;

public record CreatePrivateGameLobbyInvitesRequest(
        List<UUID> friendPublicIds
) {
}
