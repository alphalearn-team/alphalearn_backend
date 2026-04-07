package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.dto.LearnerCurrentImposterMonthlyPackDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/imposter/monthly-pack")
@Tag(name = "My Imposter Monthly Pack", description = "Learner-facing current monthly imposter pack endpoint")
public class MeImposterMonthlyPackController {

    private final LearnerImposterMonthlyPackService learnerImposterMonthlyPackService;

    public MeImposterMonthlyPackController(LearnerImposterMonthlyPackService learnerImposterMonthlyPackService) {
        this.learnerImposterMonthlyPackService = learnerImposterMonthlyPackService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current monthly imposter pack", description = "Returns learner-safe current monthly imposter pack concepts and weekly featured slots")
    public LearnerCurrentImposterMonthlyPackDto getCurrentMonthlyPack(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerImposterMonthlyPackService.getCurrentMonthlyPack(user);
    }
}
