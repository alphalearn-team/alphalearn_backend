package com.example.demo.admin.contributorapplication;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributorapplication.dto.ContributorApplicationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contributor-applications")
@Tag(name = "Admin Contributor Applications", description = "Admin-only contributor application review endpoints")
public class AdminContributorApplicationController {

    private final AdminContributorApplicationService adminContributorApplicationService;

    public AdminContributorApplicationController(AdminContributorApplicationService adminContributorApplicationService) {
        this.adminContributorApplicationService = adminContributorApplicationService;
    }

    @GetMapping("/{applicationPublicId}")
    @Operation(
            summary = "Get contributor application",
            description = "Returns a contributor application by public UUID for admin review."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contributor application returned"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Contributor application not found")
    })
    public ContributorApplicationDto getApplicationByPublicId(
            @PathVariable UUID applicationPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return adminContributorApplicationService.getApplicationByPublicId(applicationPublicId, user);
    }

    @PutMapping("/{applicationPublicId}/approve")
    @Operation(
            summary = "Approve contributor application",
            description = "Approves a PENDING contributor application and promotes/re-activates the learner as a contributor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contributor application approved"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Contributor application not found"),
            @ApiResponse(responseCode = "409", description = "Only PENDING contributor applications can be approved")
    })
    public ContributorApplicationDto approveApplication(
            @PathVariable UUID applicationPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return adminContributorApplicationService.approveApplication(applicationPublicId, user);
    }

    @PutMapping("/{applicationPublicId}/reject")
    @Operation(
            summary = "Reject contributor application",
            description = "Rejects a PENDING contributor application and records the admin rejection reason."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contributor application rejected"),
            @ApiResponse(responseCode = "400", description = "Reject reason is missing or blank"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Contributor application not found"),
            @ApiResponse(responseCode = "409", description = "Only PENDING contributor applications can be rejected")
    })
    public ContributorApplicationDto rejectApplication(
            @PathVariable UUID applicationPublicId,
            @RequestBody(
                    required = true,
                    description = "Manual rejection payload containing the admin reason.",
                    content = @Content(schema = @Schema(implementation = RejectContributorApplicationRequest.class))
            )
            @org.springframework.web.bind.annotation.RequestBody RejectContributorApplicationRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return adminContributorApplicationService.rejectApplication(applicationPublicId, request, user);
    }
}
