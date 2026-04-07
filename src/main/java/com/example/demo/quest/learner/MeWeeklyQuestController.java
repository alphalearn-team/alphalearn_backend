package com.example.demo.quest.learner;

import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/weekly-quest")
@Tag(name = "My Weekly Quest", description = "Learner-facing current weekly quest endpoint")
public class MeWeeklyQuestController {

    private final LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService;
    private final LearnerQuestChallengeUploadService learnerQuestChallengeUploadService;
    private final LearnerQuestChallengeSubmissionService learnerQuestChallengeSubmissionService;
        private final LearnerQuestChallengeFeedQueryService learnerQuestChallengeFeedQueryService;
        private final QuestHistoryQueryService questHistoryQueryService;

    public MeWeeklyQuestController(
            LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService,
            LearnerQuestChallengeUploadService learnerQuestChallengeUploadService,
            LearnerQuestChallengeSubmissionService learnerQuestChallengeSubmissionService,
            LearnerQuestChallengeFeedQueryService learnerQuestChallengeFeedQueryService,
            QuestHistoryQueryService questHistoryQueryService
    ) {
        this.learnerWeeklyQuestQueryService = learnerWeeklyQuestQueryService;
        this.learnerQuestChallengeUploadService = learnerQuestChallengeUploadService;
        this.learnerQuestChallengeSubmissionService = learnerQuestChallengeSubmissionService;
        this.learnerQuestChallengeFeedQueryService = learnerQuestChallengeFeedQueryService;
        this.questHistoryQueryService = questHistoryQueryService;
    }

    @GetMapping("/friends/feed")
    @Operation(summary = "Get friends quest challenge feed", description = "Returns paginated quest challenge submissions from the authenticated learner's friends")
    public FriendQuestChallengeFeedDto getFriendsQuestChallengeFeed(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return learnerQuestChallengeFeedQueryService.getFriendsFeed(user, page, size);
    }

    @GetMapping("/history")
    @Operation(summary = "Get my quest history", description = "Returns paginated weekly quest submissions of the authenticated learner")
    public QuestHistoryDto getMyQuestHistory(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<UUID> weekPublicIds
    ) {
        return questHistoryQueryService.getMyHistory(user, page, size, weekPublicIds);
    }

    @GetMapping("/friends/{friendPublicId}/history")
    @Operation(summary = "Get friend quest history", description = "Returns paginated weekly quest submissions of a friend with friend/public visibility")
    public QuestHistoryDto getFriendQuestHistory(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID friendPublicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<UUID> weekPublicIds
    ) {
        return questHistoryQueryService.getFriendHistory(user, friendPublicId, page, size, weekPublicIds);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weekly quest", description = "Returns the current active weekly concept and quest for the authenticated learner, or null when unavailable")
    public LearnerCurrentWeeklyQuestDto getCurrentWeeklyQuest(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerWeeklyQuestQueryService.getCurrentWeeklyQuest(user).orElse(null);
    }

    @PostMapping("/current/quest-challenge/upload")
    @Operation(summary = "Create quest challenge upload instructions", description = "Returns a presigned Cloudflare R2 upload URL for the authenticated learner's current quest challenge evidence")
    public QuestChallengeUploadResponse createQuestChallengeUpload(
            @RequestBody QuestChallengeUploadRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuestChallengeUploadService.createUploadInstruction(request, user);
    }

    @GetMapping("/current/quest-challenge/submission")
    @Operation(summary = "Get current quest challenge submission", description = "Returns the authenticated learner's saved quest challenge submission for the current active weekly quest, or null when unavailable")
    public QuestChallengeSubmissionResponse getCurrentQuestChallengeSubmission(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuestChallengeSubmissionService.getCurrentSubmission(user)
                .map(QuestChallengeSubmissionResponse::from)
                .orElse(null);
    }

    @org.springframework.web.bind.annotation.PutMapping("/current/quest-challenge/submission")
    @Operation(summary = "Save current quest challenge submission", description = "Creates or replaces the authenticated learner's current quest challenge submission for the active weekly quest")
    public QuestChallengeSubmissionResponse saveQuestChallengeSubmission(
            @RequestBody QuestChallengeSubmissionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return QuestChallengeSubmissionResponse.from(
                learnerQuestChallengeSubmissionService.saveCurrentSubmission(request.toCommand(), user)
        );
    }
}
