package com.example.demo.me.weeklyquest;

import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/weekly-quest")
@Tag(name = "My Weekly Quest", description = "Learner-facing current weekly quest endpoint")
public class MeWeeklyQuestController {

    private final LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService;
    private final LearnerQuestChallengeUploadService learnerQuestChallengeUploadService;

    public MeWeeklyQuestController(
            LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService,
            LearnerQuestChallengeUploadService learnerQuestChallengeUploadService
    ) {
        this.learnerWeeklyQuestQueryService = learnerWeeklyQuestQueryService;
        this.learnerQuestChallengeUploadService = learnerQuestChallengeUploadService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weekly quest", description = "Returns the current active weekly concept and quest for the authenticated learner, or null when unavailable")
    public LearnerCurrentWeeklyQuestDto getCurrentWeeklyQuest() {
        return learnerWeeklyQuestQueryService.getCurrentWeeklyQuest().orElse(null);
    }

    @PostMapping("/current/quest-challenge/upload")
    @Operation(summary = "Create quest challenge upload instructions", description = "Returns a presigned Cloudflare R2 upload URL for the authenticated learner's current quest challenge evidence")
    public QuestChallengeUploadResponse createQuestChallengeUpload(
            @RequestBody QuestChallengeUploadRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuestChallengeUploadService.createUploadInstruction(request, user);
    }
}
