package com.example.demo.game.lobby.invite;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.invite.dto.CreatePrivateGameLobbyInvitesRequest;
import com.example.demo.game.lobby.invite.dto.GameLobbyInviteDto;
import com.example.demo.game.lobby.invite.dto.UpdatePrivateGameLobbyInviteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/game-lobbies")
@Tag(name = "My Game Lobby Invites", description = "Learner inbox game lobby invite endpoints")
public class MeGameLobbyInviteController {

    private final GameLobbyInviteService gameLobbyInviteService;

    public MeGameLobbyInviteController(GameLobbyInviteService gameLobbyInviteService) {
        this.gameLobbyInviteService = gameLobbyInviteService;
    }

    @PostMapping("/private-lobbies/{lobbyPublicId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite friends to private game lobby", description = "Creates or refreshes pending lobby invites for selected friends")
    public List<GameLobbyInviteDto> inviteFriends(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID lobbyPublicId,
            @RequestBody CreatePrivateGameLobbyInvitesRequest request
    ) {
        return gameLobbyInviteService.inviteFriends(user, lobbyPublicId, request == null ? null : request.friendPublicIds());
    }

    @GetMapping("/private-invites")
    @Operation(summary = "List private lobby invites", description = "Returns incoming or outgoing game lobby invites for authenticated learner")
    public List<GameLobbyInviteDto> listInvites(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam String direction,
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        GameLobbyInviteDirection parsedDirection;
        GameLobbyInviteStatus parsedStatus;
        try {
            parsedDirection = GameLobbyInviteDirection.fromQueryValue(direction);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        try {
            parsedStatus = GameLobbyInviteStatus.valueOf(status == null ? "" : status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is invalid");
        }
        return gameLobbyInviteService.listInvites(user, parsedDirection, parsedStatus);
    }

    @PatchMapping("/private-invites/{invitePublicId}")
    @Operation(summary = "Respond to private lobby invite", description = "Applies ACCEPT or REJECT to a pending invite")
    public GameLobbyInviteDto respondToInvite(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID invitePublicId,
            @RequestBody UpdatePrivateGameLobbyInviteRequest request
    ) {
        return gameLobbyInviteService.respondToInvite(user, invitePublicId, request == null ? null : request.action());
    }

    @DeleteMapping("/private-invites/{invitePublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel outgoing private lobby invite", description = "Cancels a pending invite created by current user")
    public void cancelInvite(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID invitePublicId
    ) {
        gameLobbyInviteService.cancelInvite(user, invitePublicId);
    }
}
