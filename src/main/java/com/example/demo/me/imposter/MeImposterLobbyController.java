package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/imposter/lobbies")
@Tag(name = "My Imposter Lobbies", description = "Learner-only private imposter lobby endpoints")
public class MeImposterLobbyController {

    private final LearnerImposterLobbyService learnerImposterLobbyService;

    public MeImposterLobbyController(LearnerImposterLobbyService learnerImposterLobbyService) {
        this.learnerImposterLobbyService = learnerImposterLobbyService;
    }

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create private imposter lobby", description = "Creates a private imposter lobby with concept pool mode set to current month pack or full concept pool")
    public PrivateImposterLobbyDto createPrivateLobby(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestBody CreatePrivateImposterLobbyRequest request
    ) {
        return learnerImposterLobbyService.createPrivateLobby(user, request);
    }

    @PostMapping("/private/join")
    @Operation(summary = "Join private imposter lobby by code", description = "Learner joins an existing private imposter lobby using an invite code")
    public JoinedPrivateImposterLobbyDto joinPrivateLobby(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestBody JoinPrivateImposterLobbyRequest request
    ) {
        return learnerImposterLobbyService.joinPrivateLobby(user, request);
    }
}
