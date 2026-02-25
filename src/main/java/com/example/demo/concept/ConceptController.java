package com.example.demo.concept;

import java.util.List;
import java.util.UUID;

import com.example.demo.concept.dto.ConceptPublicDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/concepts")
public class ConceptController {

    // read only controller, create/update/delete endpoints are at AdminConceptController
    private final ConceptQueryService conceptQueryService;

    public ConceptController(ConceptQueryService conceptQueryService) {
        this.conceptQueryService = conceptQueryService;
    }

    @GetMapping
    public List<ConceptPublicDto> getConcepts() {
        return conceptQueryService.getAllPublicConcepts();
    }

    @GetMapping("/{conceptPublicId}")
    public ConceptPublicDto getConceptByPublicId(@PathVariable UUID conceptPublicId) {
        return conceptQueryService.getConceptByPublicId(conceptPublicId);
    }

}
