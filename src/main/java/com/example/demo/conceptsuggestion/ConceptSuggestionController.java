package com.example.demo.conceptsuggestion;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.conceptsuggestion.dto.ConceptSuggestionDto;
import com.example.demo.conceptsuggestion.dto.SaveConceptSuggestionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/concept-suggestions")
@Tag(name = "Concept Suggestions", description = "Authenticated concept suggestion draft endpoints")
public class ConceptSuggestionController {

    private final ConceptSuggestionService conceptSuggestionService;

    public ConceptSuggestionController(ConceptSuggestionService conceptSuggestionService) {
        this.conceptSuggestionService = conceptSuggestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create concept suggestion draft", description = "Creates a new concept suggestion owned by the authenticated user with DRAFT status")
    public ConceptSuggestionDto createDraft(
            @RequestBody SaveConceptSuggestionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return conceptSuggestionService.createDraft(request, user);
    }

    @PutMapping("/{conceptSuggestionPublicId}")
    @Operation(summary = "Save concept suggestion draft", description = "Updates the authenticated owner's concept suggestion while it remains in DRAFT status")
    public ConceptSuggestionDto updateDraft(
            @PathVariable UUID conceptSuggestionPublicId,
            @RequestBody SaveConceptSuggestionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return conceptSuggestionService.updateDraft(conceptSuggestionPublicId, request, user);
    }
}
