package com.example.demo.game;

import com.example.demo.game.dto.GameAssignedConceptDto;
import com.example.demo.game.dto.NextGameConceptRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.config.SupabaseAuthUser;

@RestController
@RequestMapping("/api/games/concepts")
@Tag(name = "Game Game Concepts", description = "Concept assignment endpoints for the imposter game")
public class GameConceptController {

    private final GameConceptService imposterGameConceptService;

    public GameConceptController(GameConceptService imposterGameConceptService) {
        this.imposterGameConceptService = imposterGameConceptService;
    }

    @PostMapping("/selections")
    @Operation(summary = "Assign next imposter game concept", description = "Returns one concept for a new imposter game round, excluding any provided concept public IDs")
    public GameAssignedConceptDto assignNextConcept(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestBody(required = false) NextGameConceptRequest request
    ) {
        return imposterGameConceptService.assignNextConcept(user, request);
    }
}
