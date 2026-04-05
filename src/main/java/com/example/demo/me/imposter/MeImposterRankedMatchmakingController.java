package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.me.imposter.dto.RankedImposterMatchmakingStatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/imposter/matchmaking/ranked")
@Tag(name = "My Ranked Matchmaking", description = "Learner-only ranked imposter matchmaking queue endpoints")
public class MeImposterRankedMatchmakingController {

    private final LearnerImposterRankedMatchmakingService learnerImposterRankedMatchmakingService;

    public MeImposterRankedMatchmakingController(
            LearnerImposterRankedMatchmakingService learnerImposterRankedMatchmakingService
    ) {
        this.learnerImposterRankedMatchmakingService = learnerImposterRankedMatchmakingService;
    }

    @PostMapping("/enqueue")
    @Operation(summary = "Join ranked matchmaking queue")
    public RankedImposterMatchmakingStatusDto enqueue(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerImposterRankedMatchmakingService.enqueue(user);
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel ranked matchmaking queue")
    public RankedImposterMatchmakingStatusDto cancel(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerImposterRankedMatchmakingService.cancel(user);
    }

    @GetMapping("/status")
    @Operation(summary = "Get ranked matchmaking status")
    public RankedImposterMatchmakingStatusDto status(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerImposterRankedMatchmakingService.getStatus(user);
    }
}
