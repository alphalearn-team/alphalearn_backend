package com.example.demo.conceptsuggestion;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.conceptsuggestion.dto.ConceptSuggestionDto;
import com.example.demo.conceptsuggestion.dto.SaveConceptSuggestionRequest;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid request body",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user required",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Learner record not found for the authenticated user",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ConceptSuggestionDto createDraft(
            @RequestBody(
                    required = true,
                    description = "Initial draft contents. Title and description may be partially filled while the suggestion remains in DRAFT."
            )
            @org.springframework.web.bind.annotation.RequestBody SaveConceptSuggestionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return conceptSuggestionService.createDraft(request, user);
    }

    @PutMapping("/{conceptSuggestionPublicId}")
    @Operation(summary = "Save concept suggestion draft", description = "Updates the authenticated owner's concept suggestion while it remains in DRAFT status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Draft saved"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid request body",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is not the draft owner",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Concept suggestion not found",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Concept suggestion is no longer editable because it is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ConceptSuggestionDto updateDraft(
            @Parameter(description = "Public UUID of the concept suggestion draft")
            @PathVariable UUID conceptSuggestionPublicId,
            @RequestBody(
                    required = true,
                    description = "Current draft contents to persist."
            )
            @org.springframework.web.bind.annotation.RequestBody SaveConceptSuggestionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return conceptSuggestionService.updateDraft(conceptSuggestionPublicId, request, user);
    }
}
