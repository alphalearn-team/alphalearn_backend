package com.example.demo.admin.concept;

import java.util.List;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.concept.dto.ConceptCreateDTO;
import com.example.demo.concept.dto.ConceptPublicDto;
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
public class AdminConceptController {

    private final AdminConceptFacade conceptAdminFacade;

    public AdminConceptController(AdminConceptFacade conceptAdminFacade) {
        this.conceptAdminFacade = conceptAdminFacade;
    }

    @GetMapping
    public List<ConceptPublicDto> getConcepts() {
        return conceptAdminFacade.getAllConcepts();
    }

    @GetMapping("/{publicId}")
    public ConceptPublicDto getConceptById(@PathVariable UUID publicId) {
        return conceptAdminFacade.getConceptById(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptPublicDto createConcept(@RequestBody ConceptCreateDTO concept) {
        return conceptAdminFacade.createConcept(concept);
    }

    @PutMapping("/{publicId}")
    public ConceptPublicDto updateConcept(
            @PathVariable UUID publicId,
            @RequestBody Concept updatedConcept
    ) {
        return conceptAdminFacade.updateConcept(publicId, updatedConcept);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConcept(@PathVariable UUID publicId) {
        conceptAdminFacade.deleteConcept(publicId);
    }
}
