package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.dto.SubmitGameGuessRequest;
import com.example.demo.game.lobby.dto.SubmitGameVoteRequest;
import com.example.demo.game.lobby.dto.UpsertGameDrawingSnapshotRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class MeGameLobbyRealtimeController {

    private final LearnerGameLobbyService learnerGameLobbyService;

    public MeGameLobbyRealtimeController(LearnerGameLobbyService learnerGameLobbyService) {
        this.learnerGameLobbyService = learnerGameLobbyService;
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/drawing/live")
    public void submitLiveDrawing(
            @DestinationVariable UUID lobbyPublicId,
            @Payload UpsertGameDrawingSnapshotRequest request,
            Principal principal
    ) {
        learnerGameLobbyService.submitLiveDrawing(requireLearnerPrincipal(principal), lobbyPublicId, request);
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/drawing/done")
    public void submitDrawingDone(
            @DestinationVariable UUID lobbyPublicId,
            @Payload UpsertGameDrawingSnapshotRequest request,
            Principal principal
    ) {
        learnerGameLobbyService.submitRealtimeDrawingDone(requireLearnerPrincipal(principal), lobbyPublicId, request);
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/vote")
    public void submitVote(
            @DestinationVariable UUID lobbyPublicId,
            @Payload SubmitGameVoteRequest request,
            Principal principal
    ) {
        learnerGameLobbyService.submitRealtimeVote(requireLearnerPrincipal(principal), lobbyPublicId, request);
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/guess")
    public void submitGuess(
            @DestinationVariable UUID lobbyPublicId,
            @Payload SubmitGameGuessRequest request,
            Principal principal
    ) {
        learnerGameLobbyService.submitRealtimeGuess(requireLearnerPrincipal(principal), lobbyPublicId, request);
    }

    private SupabaseAuthUser requireLearnerPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof SupabaseAuthUser authUser
                && authUser.userId() != null
                && authUser.learner() != null) {
            return authUser;
        }
        throw new AccessDeniedException("Learner authentication required for imposter gameplay commands");
    }
}
