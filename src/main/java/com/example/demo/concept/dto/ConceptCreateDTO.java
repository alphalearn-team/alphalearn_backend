package com.example.demo.concept.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConceptCreateRequest", description = "Request payload to create a concept")
public record ConceptCreateDTO(
        @Schema(description = "Concept title", example = "Linear Equations")
        String title,
        @Schema(description = "Concept description", example = "Foundations of solving one-variable equations")
        String description
) {}
