package com.example.demo.learner.read;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.dto.LearnerProfileDto;
import com.example.demo.learner.dto.LearnerPublicDto;

@RestController
@RequestMapping("/api/learners")
@Tag(name = "Learners (Public)", description = "Read-only learner endpoints available to authenticated non-admin users")
public class LearnerController {
    
    private final LearnerQueryService learnerQueryService;

    public LearnerController(LearnerQueryService learnerQueryService) {
        this.learnerQueryService = learnerQueryService;
    }

    @GetMapping
    @Operation(summary = "List learners", description = "Returns public learner profiles from the learners table")
    public List<LearnerPublicDto> getLearners() {
        return learnerQueryService.getAllPublicLearners();
    }

    @GetMapping("/{learnerPublicId}")
    @Operation(summary = "Get learner profile", description = "Returns public profile details for the selected learner")
    public LearnerProfileDto getLearnerProfile(
            @PathVariable UUID learnerPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQueryService.getLearnerProfile(user, learnerPublicId);
    }

}
