package com.example.demo.contributor;


import java.util.List;

import com.example.demo.contributor.dto.ContributorPublicDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contributors")
@Tag(name = "Contributors (Public)", description = "Read-only contributor endpoints available to authenticated non-admin users")
public class ContributorController {
    
    private final ContributorQueryService contributorQueryService;

    public ContributorController(ContributorQueryService contributorQueryService) {
        this.contributorQueryService = contributorQueryService;
    }

    @GetMapping
    @Operation(summary = "List contributors", description = "Returns contributor status for users in the contributors table")
    public List<ContributorPublicDto> getContributors() {
        return contributorQueryService.getAllPublicContributors();
    }
}
