package com.example.demo.game.imposter;

import com.example.demo.game.imposter.dto.ImposterAssignedConceptDto;
import com.example.demo.game.imposter.dto.NextImposterConceptRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/imposter/concepts")
@Tag(name = "Imposter Game Concepts", description = "Concept assignment endpoints for the imposter game")
public class ImposterGameConceptController {

    private final ImposterGameConceptService imposterGameConceptService;

    public ImposterGameConceptController(ImposterGameConceptService imposterGameConceptService) {
        this.imposterGameConceptService = imposterGameConceptService;
    }

    @PostMapping("/next")
    @Operation(summary = "Assign next imposter game concept", description = "Returns one concept for a new imposter game round, excluding any provided concept public IDs")
    public ImposterAssignedConceptDto assignNextConcept(
            @RequestBody(required = false) NextImposterConceptRequest request
    ) {
        return imposterGameConceptService.assignNextConcept(request);
    }
}
