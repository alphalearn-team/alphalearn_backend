package com.example.demo.admin.contributor;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.demo.contributor.dto.ContributorPublicDto;
import com.example.demo.contributor.dto.DemoteContributorsRequest;
import com.example.demo.contributor.dto.PromoteContributorsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contributors")
@Tag(name = "Admin Contributors", description = "Admin-only contributor promotion and demotion endpoints")
public class AdminContributorController {

    private final AdminContributorService contributorAdminFacade;

    public AdminContributorController(AdminContributorService contributorAdminFacade) {
        this.contributorAdminFacade = contributorAdminFacade;
    }

    @GetMapping
    @Operation(summary = "List contributors (admin)", description = "Returns all contributor records including active/inactive state")
    public List<ContributorPublicDto> getContributors() {
        return contributorAdminFacade.getAllContributors();
    }

    @PostMapping("/promote")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Promote learners", description = "Creates or reactivates contributor records for the provided learner public IDs")
    public List<ContributorPublicDto> promoteLearners(@RequestBody PromoteContributorsRequest request) {
        return contributorAdminFacade.promoteLearners(request);
    }

    @DeleteMapping("/demote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Demote contributors", description = "Demotes contributors and unpublishes their non-deleted lessons")
    public void demoteContributors(@RequestBody DemoteContributorsRequest request) {
        contributorAdminFacade.demoteContributors(request);
    }
}
