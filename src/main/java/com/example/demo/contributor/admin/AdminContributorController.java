package com.example.demo.contributor.admin;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.demo.contributor.dto.ContributorPublicDto;
import com.example.demo.contributor.dto.DemoteContributorsRequest;
import com.example.demo.contributor.dto.PromoteContributorsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    @PatchMapping
    @Operation(summary = "Apply contributor role action", description = "Applies role action PROMOTE or DEMOTE for contributors")
    public ResponseEntity<?> applyContributorRoleAction(@RequestBody AdminContributorRoleActionRequest request) {
        String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase();
        return switch (action) {
            case "PROMOTE" -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(contributorAdminFacade.promoteLearners(new PromoteContributorsRequest(request.learnerPublicIds())));
            case "DEMOTE" -> {
                contributorAdminFacade.demoteContributors(new DemoteContributorsRequest(request.contributorPublicIds()));
                yield ResponseEntity.noContent().build();
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported action");
        };
    }
}
