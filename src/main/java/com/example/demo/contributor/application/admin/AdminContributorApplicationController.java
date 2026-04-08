package com.example.demo.contributor.application.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.application.dto.ContributorApplicationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/contributor-applications")
@Tag(name = "Admin Contributor Applications", description = "Admin-only contributor application review endpoints")
public class AdminContributorApplicationController {

    private final AdminContributorApplicationService adminContributorApplicationService;

    public AdminContributorApplicationController(AdminContributorApplicationService adminContributorApplicationService) {
        this.adminContributorApplicationService = adminContributorApplicationService;
    }

    @GetMapping
    @Operation(
            summary = "List contributor applications by status",
            description = "Returns contributor applications by status. Currently supported status: PENDING."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending contributor applications returned"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required")
    })
    public List<ContributorApplicationDto> getPendingApplications(
            @RequestParam String status,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"PENDING".equals(normalized)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "status must be PENDING");
        }
        return adminContributorApplicationService.getPendingApplications(user);
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

    @PatchMapping("/{applicationPublicId}")
    @Operation(summary = "Moderate contributor application", description = "Applies moderation action APPROVE or REJECT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contributor application updated"),
            @ApiResponse(responseCode = "400", description = "Invalid moderation action payload"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Contributor application not found"),
            @ApiResponse(responseCode = "409", description = "Only PENDING contributor applications can be moderated")
    })
    public ContributorApplicationDto moderateApplication(
            @PathVariable UUID applicationPublicId,
            @RequestBody(
                    required = true,
                    description = "Contributor application moderation action payload.",
                    content = @Content(schema = @Schema(implementation = AdminContributorApplicationModerationActionRequest.class))
            )
            @org.springframework.web.bind.annotation.RequestBody AdminContributorApplicationModerationActionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase();
        return switch (action) {
            case "APPROVE" -> adminContributorApplicationService.approveApplication(applicationPublicId, user);
            case "REJECT" -> adminContributorApplicationService.rejectApplication(
                    applicationPublicId,
                    new RejectContributorApplicationRequest(request.reason()),
                    user
            );
            default -> throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported action");
        };
    }
}
