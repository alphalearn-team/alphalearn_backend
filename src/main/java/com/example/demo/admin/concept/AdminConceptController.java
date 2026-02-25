package com.example.demo.admin.concept;

import com.example.demo.concept.Concept;
import com.example.demo.concept.dto.ConceptCreateDTO;
import com.example.demo.concept.dto.ConceptDTO;
import org.springframework.http.HttpStatus;
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
public class ConceptAdminController {

    private final AdminConceptFacade conceptAdminFacade;

    public ConceptAdminController(AdminConceptFacade conceptAdminFacade) {
        this.conceptAdminFacade = conceptAdminFacade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptDTO createConcept(@RequestBody ConceptCreateDTO concept) {
        return conceptAdminFacade.createConcept(concept);
    }

    @PutMapping("/{id}")
    public ConceptDTO updateConcept(
            @PathVariable Integer id,
            @RequestBody Concept updatedConcept
    ) {
        return conceptAdminFacade.updateConcept(id, updatedConcept);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConcept(@PathVariable Integer id) {
        conceptAdminFacade.deleteConcept(id);
    }
}
