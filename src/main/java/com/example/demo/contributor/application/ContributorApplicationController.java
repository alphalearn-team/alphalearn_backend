package com.example.demo.contributor.application;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.application.dto.ContributorApplicationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/contributor-applications")
@Tag(name = "Contributor Applications", description = "Authenticated learner endpoints for requesting contributor access")
public class ContributorApplicationController {

    private final ContributorApplicationService contributorApplicationService;

    public ContributorApplicationController(ContributorApplicationService contributorApplicationService) {
        this.contributorApplicationService = contributorApplicationService;
    }

    @GetMapping("/mine")
    @Operation(summary = "List my contributor applications", description = "Returns contributor applications for the authenticated learner ordered by most recently submitted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications returned"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user required",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Learner record not found for the authenticated user",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public List<ContributorApplicationDto> getMyApplications(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return contributorApplicationService.getMyApplications(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit contributor application", description = "Creates a new PENDING contributor application for the authenticated learner")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user required",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Learner record not found for the authenticated user",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Learner is already a contributor or has a pending application",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ContributorApplicationDto submitApplication(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return contributorApplicationService.submitApplication(user);
    }
}
