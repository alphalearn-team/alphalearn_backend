package com.example.demo.admin.conceptsuggestion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/concept-suggestions")
@Tag(name = "Admin Concept Suggestions", description = "Admin-only concept suggestion review queue endpoints")
public class AdminConceptSuggestionController {

    private final AdminConceptSuggestionService adminConceptSuggestionService;

    public AdminConceptSuggestionController(AdminConceptSuggestionService adminConceptSuggestionService) {
        this.adminConceptSuggestionService = adminConceptSuggestionService;
    }

    @GetMapping
    @Operation(summary = "List submitted concept suggestions", description = "Returns the submitted concept suggestion review queue ordered by oldest submission first")
    public List<AdminConceptSuggestionQueueItemDto> getSubmittedSuggestions() {
        return adminConceptSuggestionService.getSubmittedSuggestions();
    }
}
