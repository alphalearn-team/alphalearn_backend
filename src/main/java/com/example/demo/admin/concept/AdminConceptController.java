package com.example.demo.admin.concept;

import java.util.List;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.dto.ConceptCreateDTO;
import com.example.demo.concept.dto.ConceptPublicDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/concepts")
@Tag(name = "Admin Concepts", description = "Admin-only concept management endpoints")
public class AdminConceptController {

    private final AdminConceptService conceptAdminFacade;

    public AdminConceptController(AdminConceptService conceptAdminFacade) {
        this.conceptAdminFacade = conceptAdminFacade;
    }

    @GetMapping
    @Operation(summary = "List concepts (admin)", description = "Returns all concepts for admin management")
    public List<ConceptPublicDto> getConcepts() {
        return conceptAdminFacade.getAllConcepts();
    }

    @GetMapping("/{conceptPublicId}")
    @Operation(summary = "Get concept (admin)", description = "Returns a concept by public ID for admin review")
    public ConceptPublicDto getConceptByPublicId(@PathVariable UUID conceptPublicId) {
        return conceptAdminFacade.getConceptByPublicId(conceptPublicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create concept", description = "Creates a new concept row")
    public ConceptPublicDto createConcept(@RequestBody ConceptCreateDTO concept) {
        return conceptAdminFacade.createConcept(concept);
    }

    @PutMapping("/{conceptPublicId}")
    @Operation(summary = "Update concept", description = "Updates title and description for a concept")
    public ConceptPublicDto updateConcept(
            @PathVariable UUID conceptPublicId,
            @RequestBody Concept updatedConcept
    ) {
        return conceptAdminFacade.updateConcept(conceptPublicId, updatedConcept);
    }

    @DeleteMapping("/{conceptPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete concept", description = "Deletes a concept only when no lessons are attached")
    public void deleteConcept(
            @PathVariable UUID conceptPublicId,
            org.springframework.security.core.Authentication authentication
    ) {
        System.out.println("=== DELETE CONCEPT DEBUG ===");
        System.out.println("conceptPublicId: " + conceptPublicId);
        System.out.println("Authentication: " + authentication);
        System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
        System.out.println("Authorities: " + (authentication != null ? authentication.getAuthorities() : "null"));
        System.out.println("===========================");
        conceptAdminFacade.deleteConcept(conceptPublicId);
    }
}
