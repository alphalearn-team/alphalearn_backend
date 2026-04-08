package com.example.demo.quest.learner;

import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/weekly-quests")
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

    @GetMapping("/entries")
    @Operation(summary = "Get quest entries", description = "Returns quest entries by view. Supported view: FEED. Supports optional filtering by week IDs and submitted time range.")
    public FriendQuestChallengeFeedDto getQuestEntries(
            @RequestParam String view,
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<UUID> weekPublicIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedTo
    ) {
        String normalized = view == null ? "" : view.trim().toUpperCase();
        if (!"FEED".equals(normalized)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "view must be FEED");
        }
        return learnerQuestChallengeFeedQueryService.getFriendsFeed(user, page, size, weekPublicIds, submittedFrom, submittedTo);
    }

    @GetMapping("/history")
    @Operation(summary = "Get my quest history", description = "Returns paginated weekly quest submissions of the authenticated learner. Supports optional filtering by week IDs and submitted time range.")
    public QuestHistoryDto getMyQuestHistory(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) UUID friendPublicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<UUID> weekPublicIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedTo
    ) {
        return questHistoryQueryService.getMyHistory(user, page, size, weekPublicIds, submittedFrom, submittedTo);
    }

    @GetMapping("/friends/{friendPublicId}/history")
    @Operation(summary = "Get friend quest history", description = "Returns paginated weekly quest submissions of a friend with friend/public visibility. Supports optional filtering by week IDs and submitted time range.")
    public QuestHistoryDto getFriendQuestHistory(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @PathVariable UUID friendPublicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<UUID> weekPublicIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedTo
    ) {
        return questHistoryQueryService.getFriendHistory(user, friendPublicId, page, size, weekPublicIds, submittedFrom, submittedTo);
    }

    @GetMapping
    @Operation(summary = "Get current weekly quest", description = "Returns the current active weekly concept and quest for the authenticated learner, or null when unavailable")
    public LearnerCurrentWeeklyQuestDto getCurrentWeeklyQuest(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerWeeklyQuestQueryService.getCurrentWeeklyQuest(user).orElse(null);
    }

    @PostMapping("/quest-challenge-uploads")
    @Operation(summary = "Create quest challenge upload instructions", description = "Returns a presigned Cloudflare R2 upload URL for the authenticated learner's current quest challenge evidence")
    public QuestChallengeUploadResponse createQuestChallengeUpload(
            @RequestBody QuestChallengeUploadRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuestChallengeUploadService.createUploadInstruction(request, user);
    }

    @GetMapping(value = "/quest-challenge-submissions", params = "scope=CURRENT")
    @Operation(summary = "Get quest challenge submission by scope", description = "Returns quest challenge submission for supported scope CURRENT, or null when unavailable")
    public QuestChallengeSubmissionResponse getCurrentQuestChallengeSubmission(
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuestChallengeSubmissionService.getCurrentSubmission(user)
                .map(QuestChallengeSubmissionResponse::from)
                .orElse(null);
    }

    @org.springframework.web.bind.annotation.PutMapping(value = "/quest-challenge-submissions", params = "scope=CURRENT")
    @Operation(summary = "Save quest challenge submission by scope", description = "Creates or replaces quest challenge submission for supported scope CURRENT")
    public QuestChallengeSubmissionResponse saveQuestChallengeSubmission(
            @RequestBody QuestChallengeSubmissionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return QuestChallengeSubmissionResponse.from(
                learnerQuestChallengeSubmissionService.saveCurrentSubmission(request.toCommand(), user)
        );
    }
}
