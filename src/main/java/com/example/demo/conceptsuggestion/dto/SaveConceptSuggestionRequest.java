package com.example.demo.conceptsuggestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SaveConceptSuggestionRequest", description = "Request payload used to create or save a concept suggestion draft")
public record SaveConceptSuggestionRequest(
        @Schema(description = "Draft title. Optional while the suggestion remains a draft.", example = "Fractions through pizza sharing")
        String title,
        @Schema(description = "Draft description. Optional while the suggestion remains a draft.", example = "An idea for teaching fractions visually through portions and sharing scenarios")
        String description
) {}
