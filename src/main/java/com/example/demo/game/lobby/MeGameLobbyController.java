package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.dto.CreatePrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.GameLobbyTransitionActionRequest;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import com.example.demo.game.lobby.dto.UpdatePrivateGameLobbySettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/game-lobbies")
@Tag(name = "My Game Lobbies", description = "Learner-only private imposter lobby endpoints")
public class MeGameLobbyController {

    private final LearnerGameLobbyService learnerGameLobbyService;

    public MeGameLobbyController(LearnerGameLobbyService learnerGameLobbyService) {
        this.learnerGameLobbyService = learnerGameLobbyService;
    }

    @PostMapping("/private-lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create private imposter lobby", description = "Creates a private imposter lobby with concept pool mode set to current month pack or full concept pool")
    public PrivateGameLobbyDto createPrivateLobby(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestBody CreatePrivateGameLobbyRequest request
    ) {
        return learnerGameLobbyService.createPrivateLobby(user, request);
    }

    @PostMapping("/private-memberships")
    @Operation(summary = "Join private imposter lobby by code", description = "Learner joins an existing private imposter lobby using an invite code")
    public JoinedPrivateGameLobbyDto joinPrivateLobby(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestBody JoinPrivateGameLobbyRequest request
    ) {
        return learnerGameLobbyService.joinPrivateLobby(user, request);
    }

    @PatchMapping("/private-lobbies/{lobbyPublicId}")
    @Operation(summary = "Apply private lobby transition action", description = "Applies START or LEAVE action to private lobby")
    public Object applyLobbyTransition(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID lobbyPublicId,
            @RequestBody GameLobbyTransitionActionRequest request
    ) {
        String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase();
        return switch (action) {
            case "START" -> learnerGameLobbyService.startPrivateLobby(user, lobbyPublicId);
            case "LEAVE" -> learnerGameLobbyService.leavePrivateLobby(user, lobbyPublicId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported action");
        };
    }

    @PatchMapping("/private-lobbies/{lobbyPublicId}/settings")
    @Operation(summary = "Update private imposter lobby settings", description = "Host updates pre-game settings before the lobby starts")
    public PrivateGameLobbyStateDto updatePrivateLobbySettings(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID lobbyPublicId,
            @RequestBody UpdatePrivateGameLobbySettingsRequest request
    ) {
        return learnerGameLobbyService.updatePrivateLobbySettings(user, lobbyPublicId, request);
    }

    @GetMapping("/private-lobbies/{lobbyPublicId}")
    @Operation(summary = "Get private imposter lobby state", description = "Returns live lobby state snapshot for polling clients")
    public PrivateGameLobbyStateDto getPrivateLobbyState(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID lobbyPublicId
    ) {
        return learnerGameLobbyService.getPrivateLobbyState(user, lobbyPublicId);
    }
}
