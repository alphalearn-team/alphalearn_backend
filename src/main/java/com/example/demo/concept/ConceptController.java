package com.example.demo.concept;

import java.util.List;
import java.util.UUID;

import com.example.demo.concept.dto.ConceptPublicDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/concepts")
@Tag(name = "Concepts (Public)", description = "Read-only concept endpoints available to authenticated non-admin users")
public class ConceptController {

    // read only controller, create/update/delete endpoints are at AdminConceptController
    private final ConceptQueryService conceptQueryService;

    public ConceptController(ConceptQueryService conceptQueryService) {
        this.conceptQueryService = conceptQueryService;
    }

    @GetMapping
    @Operation(summary = "List concepts", description = "Returns all concepts from the concepts table")
    public List<ConceptPublicDto> getConcepts() {
        return conceptQueryService.getAllPublicConcepts();
    }

    @GetMapping("/{conceptPublicId}")
    @Operation(summary = "Get concept by public ID", description = "Returns one concept by UUID publicId")
    public ConceptPublicDto getConceptByPublicId(@PathVariable UUID conceptPublicId) {
        return conceptQueryService.getConceptByPublicId(conceptPublicId);
    }

}
