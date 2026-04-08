package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.dto.LearnerCurrentGameMonthlyPackDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/game-monthly-pack")
@Tag(name = "My Game Monthly Pack", description = "Learner-facing current monthly imposter pack endpoint")
public class MeGameMonthlyPackController {

    private final LearnerGameMonthlyPackService learnerGameMonthlyPackService;

    public MeGameMonthlyPackController(LearnerGameMonthlyPackService learnerGameMonthlyPackService) {
        this.learnerGameMonthlyPackService = learnerGameMonthlyPackService;
    }

    @GetMapping
    @Operation(summary = "Get current monthly imposter pack", description = "Returns learner-safe current monthly imposter pack concepts and weekly featured slots")
    public LearnerCurrentGameMonthlyPackDto getCurrentMonthlyPack(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerGameMonthlyPackService.getCurrentMonthlyPack(user);
    }
}
