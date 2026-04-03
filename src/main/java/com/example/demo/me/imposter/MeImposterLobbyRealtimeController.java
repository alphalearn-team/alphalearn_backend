package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.me.imposter.dto.SubmitImposterGuessRequest;
import com.example.demo.me.imposter.dto.SubmitImposterVoteRequest;
import com.example.demo.me.imposter.dto.UpsertImposterDrawingSnapshotRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class MeImposterLobbyRealtimeController {

    private final LearnerImposterLobbyService learnerImposterLobbyService;

    public MeImposterLobbyRealtimeController(LearnerImposterLobbyService learnerImposterLobbyService) {
        this.learnerImposterLobbyService = learnerImposterLobbyService;
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/drawing/live")
    public void submitLiveDrawing(
            @DestinationVariable UUID lobbyPublicId,
            @Payload UpsertImposterDrawingSnapshotRequest request,
            Principal principal
    ) {
        learnerImposterLobbyService.submitLiveDrawing(requireLearnerPrincipal(principal), lobbyPublicId, request);
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/vote")
    public void submitVote(
            @DestinationVariable UUID lobbyPublicId,
            @Payload SubmitImposterVoteRequest request,
            Principal principal
    ) {
        learnerImposterLobbyService.submitRealtimeVote(requireLearnerPrincipal(principal), lobbyPublicId, request);
    }

    @MessageMapping("/imposter/lobbies/{lobbyPublicId}/guess")
    public void submitGuess(
            @DestinationVariable UUID lobbyPublicId,
            @Payload SubmitImposterGuessRequest request,
            Principal principal
    ) {
        learnerImposterLobbyService.submitRealtimeGuess(requireLearnerPrincipal(principal), lobbyPublicId, request);
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
