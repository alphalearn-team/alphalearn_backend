package com.example.demo.contributor.application;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.application.dto.ContributorApplicationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/contributor-applications")
@Tag(name = "My Contributor Applications", description = "Authenticated learner contributor application listing endpoint")
public class MeContributorApplicationController {

    private final ContributorApplicationService contributorApplicationService;

    public MeContributorApplicationController(ContributorApplicationService contributorApplicationService) {
        this.contributorApplicationService = contributorApplicationService;
    }

    @GetMapping
    @Operation(summary = "List my contributor applications", description = "Returns contributor applications for the authenticated learner ordered by most recently submitted")
    public List<ContributorApplicationDto> getMyApplications(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return contributorApplicationService.getMyApplications(user);
    }
}
