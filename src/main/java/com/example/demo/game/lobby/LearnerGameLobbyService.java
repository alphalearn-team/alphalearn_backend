package com.example.demo.game.lobby;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.GameWeeklyFeaturedConceptService;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.GameLobbyCodeGenerator;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import com.example.demo.game.lobby.GameLobbyPhase;
import com.example.demo.game.realtime.GameLobbyRealtimePublisher;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.lobby.invite.GameLobbyInviteRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.game.lobby.dto.CreatePrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.GameConceptResolution;
import com.example.demo.game.lobby.dto.GameConceptWinnerSide;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.LeavePrivateGameLobbyResponse;
import com.example.demo.game.lobby.dto.PrivateGameLobbyConceptResultDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyLeaveResult;
import com.example.demo.game.lobby.dto.PrivateGameLobbyMemberStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyPlayerScoreDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyReconnectStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbySharedStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyViewerStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyVoteTallyDto;
import com.example.demo.game.lobby.dto.SubmitGameGuessRequest;
import com.example.demo.game.lobby.dto.SubmitGameVoteRequest;
import com.example.demo.game.lobby.dto.UpdatePrivateGameLobbySettingsRequest;
import com.example.demo.game.lobby.dto.UpsertGameDrawingSnapshotRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerGameLobbyService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int LOBBY_CODE_MAX_RETRIES = 10;
    private static final int MIN_ACTIVE_MEMBERS_TO_START = 3;
    private static final int DEFAULT_CONCEPT_COUNT = 3;
    private static final int DEFAULT_ROUNDS_PER_CONCEPT = 1;
    private static final int DEFAULT_DISCUSSION_TIMER_SECONDS = 30;
    private static final int DEFAULT_IMPOSTER_GUESS_TIMER_SECONDS = 30;
    private static final int DEFAULT_TURN_DURATION_SECONDS = 25;
    private static final int MAX_CONCEPT_COUNT = 10;
    private static final int MAX_ROUNDS_PER_CONCEPT = 3;
    private static final int MIN_TIMER_SECONDS = 10;
    private static final int MAX_TIMER_SECONDS = 120;
    private static final int DEFAULT_MAX_VOTING_ROUNDS = 3;
    private static final int CONCEPT_RESULT_DURATION_SECONDS = 5;
    private static final String LOBBY_CODE_UNIQUE_CONSTRAINT = "uk_imposter_game_lobbies_lobby_code";
    private static final String MEMBER_UNIQUE_CONSTRAINT = "uk_imposter_game_lobby_members_lobby_learner";
    private static final String DRAWING_VERSION_CONFLICT_MESSAGE = "Drawing version conflict";
    private static final String END_REASON_PLAYER_QUIT = "PLAYER_QUIT";
    private static final String END_REASON_PLAYER_DISCONNECTED_TIMEOUT = "PLAYER_DISCONNECTED_TIMEOUT";

    private final GameLobbyRepository imposterGameLobbyRepository;
    private final GameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final GameLobbyCodeGenerator imposterLobbyCodeGenerator;
    private final GameMonthlyPackRepository imposterMonthlyPackRepository;
    private final GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final GameLobbyInviteRepository gameLobbyInviteRepository;
    private final GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final ConceptRepository conceptRepository;
    private final LearnerRepository learnerRepository;
    private final GameLobbyRealtimePresenceTracker realtimePresenceTracker;
    private final Clock clock;
    private final GameLobbySerializationSupport serializationSupport;
    private final GameLobbyValidationSupport validationSupport;
    private final GameLobbyLifecycleSupport lifecycleSupport;
    private final GameLobbyRoundEngine roundEngine;
    private final GameLobbyStateAssembler stateAssembler;
    private final GameLobbyRealtimeSupport realtimeSupport;
    private final GameLobbyGameFlowSupport gameFlowSupport;
    private final GameLobbyOperationsSupport operationsSupport;

    public LearnerGameLobbyService(
            GameLobbyRepository imposterGameLobbyRepository,
            GameLobbyMemberRepository imposterGameLobbyMemberRepository,
            GameLobbyCodeGenerator imposterLobbyCodeGenerator,
            GameMonthlyPackRepository imposterMonthlyPackRepository,
            GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            GameLobbyInviteRepository gameLobbyInviteRepository,
            GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService,
            ConceptRepository conceptRepository,
            LearnerRepository learnerRepository,
            GameLobbyRealtimePublisher realtimePublisher,
            GameLobbyRealtimePresenceTracker realtimePresenceTracker,
            Clock clock,
            @Value("${imposter.lobby.live-drawing.enabled:false}") boolean liveDrawingEnabled
    ) {
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.imposterLobbyCodeGenerator = imposterLobbyCodeGenerator;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.gameLobbyInviteRepository = gameLobbyInviteRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
        this.conceptRepository = conceptRepository;
        this.learnerRepository = learnerRepository;
        this.realtimePresenceTracker = realtimePresenceTracker;
        this.clock = clock;
        this.serializationSupport = new GameLobbySerializationSupport();
        this.validationSupport = new GameLobbyValidationSupport(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                DRAWING_VERSION_CONFLICT_MESSAGE
        );
        this.lifecycleSupport = new GameLobbyLifecycleSupport(
                imposterGameLobbyMemberRepository,
                clock,
                LOBBY_CODE_UNIQUE_CONSTRAINT,
                MEMBER_UNIQUE_CONSTRAINT
        );
        this.roundEngine = new GameLobbyRoundEngine(serializationSupport, CONCEPT_RESULT_DURATION_SECONDS);
        this.stateAssembler = new GameLobbyStateAssembler(
                learnerRepository,
                imposterGameLobbyMemberRepository,
                realtimePresenceTracker,
                serializationSupport,
                MIN_ACTIVE_MEMBERS_TO_START,
                MIN_TIMER_SECONDS,
                MAX_TIMER_SECONDS,
                DEFAULT_CONCEPT_COUNT,
                DEFAULT_TURN_DURATION_SECONDS,
                DEFAULT_MAX_VOTING_ROUNDS
        );
        this.realtimeSupport = new GameLobbyRealtimeSupport(
                imposterGameLobbyMemberRepository,
                stateAssembler,
                realtimePublisher
        );
        this.gameFlowSupport = new GameLobbyGameFlowSupport(
                lifecycleSupport,
                serializationSupport,
                roundEngine,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                imposterWeeklyFeaturedConceptService,
                conceptRepository,
                DEFAULT_CONCEPT_COUNT,
                DEFAULT_ROUNDS_PER_CONCEPT,
                DEFAULT_DISCUSSION_TIMER_SECONDS,
                DEFAULT_IMPOSTER_GUESS_TIMER_SECONDS,
                DEFAULT_TURN_DURATION_SECONDS,
                DEFAULT_MAX_VOTING_ROUNDS
        );
        this.operationsSupport = new GameLobbyOperationsSupport(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                imposterLobbyCodeGenerator,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                gameLobbyInviteRepository,
                validationSupport,
                lifecycleSupport,
                serializationSupport,
                stateAssembler,
                realtimeSupport,
                gameFlowSupport,
                roundEngine,
                realtimePresenceTracker,
                clock,
                liveDrawingEnabled,
                LOBBY_CODE_MAX_RETRIES,
                MIN_ACTIVE_MEMBERS_TO_START,
                DEFAULT_CONCEPT_COUNT,
                DEFAULT_ROUNDS_PER_CONCEPT,
                DEFAULT_DISCUSSION_TIMER_SECONDS,
                DEFAULT_IMPOSTER_GUESS_TIMER_SECONDS,
                DEFAULT_TURN_DURATION_SECONDS,
                DEFAULT_MAX_VOTING_ROUNDS,
                MIN_TIMER_SECONDS,
                MAX_TIMER_SECONDS,
                END_REASON_PLAYER_QUIT,
                END_REASON_PLAYER_DISCONNECTED_TIMEOUT
        );
    }

    @Transactional
    public PrivateGameLobbyDto createPrivateLobby(
            SupabaseAuthUser user,
            CreatePrivateGameLobbyRequest request
    ) {
        return operationsSupport.createPrivateLobby(user, request);
    }

    @Transactional
    public JoinedPrivateGameLobbyDto joinPrivateLobby(SupabaseAuthUser user, JoinPrivateGameLobbyRequest request) {
        return operationsSupport.joinPrivateLobby(user, request);
    }

    @Transactional
    public LeavePrivateGameLobbyResponse leavePrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        return operationsSupport.leavePrivateLobby(user, lobbyPublicId);
    }

    @Transactional
    public PrivateGameLobbyStateDto updatePrivateLobbySettings(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpdatePrivateGameLobbySettingsRequest request
    ) {
        return operationsSupport.updatePrivateLobbySettings(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto startPrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        return operationsSupport.startPrivateLobby(user, lobbyPublicId);
    }

    @Transactional
    public PrivateGameLobbyStateDto getPrivateLobbyState(SupabaseAuthUser user, UUID lobbyPublicId) {
        return operationsSupport.getPrivateLobbyState(user, lobbyPublicId);
    }

    @Transactional
    public PrivateGameLobbyStateDto kickPrivateLobbyMember(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UUID memberPublicId
    ) {
        return operationsSupport.kickPrivateLobbyMember(user, lobbyPublicId, memberPublicId);
    }

    @Transactional
    public PrivateGameLobbyStateDto upsertDrawingSnapshot(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertGameDrawingSnapshotRequest request
    ) {
        return operationsSupport.upsertDrawingSnapshot(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertGameDrawingSnapshotRequest request
    ) {
        return operationsSupport.submitDrawingDone(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitGameVoteRequest request
    ) {
        return operationsSupport.submitVote(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitGameGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitGameGuessRequest request
    ) {
        return operationsSupport.submitGameGuess(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitLiveDrawing(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertGameDrawingSnapshotRequest request
    ) {
        return upsertDrawingSnapshot(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitRealtimeVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitGameVoteRequest request
    ) {
        return submitVote(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitRealtimeGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitGameGuessRequest request
    ) {
        return submitGameGuess(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateGameLobbyStateDto submitRealtimeDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertGameDrawingSnapshotRequest request
    ) {
        return submitDrawingDone(user, lobbyPublicId, request);
    }

    @Transactional
    public void processRealtimeTimedTransitions() {
        operationsSupport.processRealtimeTimedTransitions();
    }

    @Transactional
    public void processRealtimeDisconnectTimeouts() {
        operationsSupport.processRealtimeDisconnectTimeouts();
    }

    @Transactional
    public void publishRealtimePresenceUpdate(UUID lobbyPublicId, String reason) {
        operationsSupport.publishRealtimePresenceUpdate(lobbyPublicId, reason);
    }

    private void initializeConceptRuntime(GameLobby lobby, List<GameLobbyMember> activeMembers, OffsetDateTime now) {
        gameFlowSupport.initializeConceptRuntime(lobby, activeMembers, now);
    }

    private boolean resolveTimedTransitionsIfNeeded(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        return gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
    }

    private void advanceToNextDrawStepOrVoting(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        gameFlowSupport.advanceToNextDrawStepOrVoting(lobby, activeMembers, now);
    }

    private void finalizeVotingRound(GameLobby lobby, List<GameLobbyMember> activeMembers, OffsetDateTime now) {
        gameFlowSupport.finalizeVotingRound(lobby, activeMembers, now);
    }

    private void resolveConceptOutcome(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now,
            GameLobbyGameFlowSupport.WinnerSide winnerSide,
            GameLobbyGameFlowSupport.ConceptResolution resolution,
            UUID accusedLearnerId,
            String imposterGuess,
            boolean imposterWinsByVotingTie,
            Map<UUID, Integer> voteTallies
    ) {
        gameFlowSupport.resolveConceptOutcome(
                lobby,
                activeMembers,
                now,
                winnerSide,
                resolution,
                accusedLearnerId,
                imposterGuess,
                imposterWinsByVotingTie,
                voteTallies
        );
    }

    private void handleActiveGameOnMemberLeave(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            UUID leftLearnerId,
            OffsetDateTime now
    ) {
        gameFlowSupport.handleActiveGameOnMemberLeave(lobby, activeMembers, leftLearnerId, now);
    }

    private void abandonLobby(
            GameLobby lobby,
            UUID abandonedByLearnerId,
            OffsetDateTime endedAt,
            String endedReason
    ) {
        roundEngine.abandonLobby(lobby, abandonedByLearnerId, endedAt, endedReason);
    }

    private GameLobby resolveLobbyByPublicId(UUID lobbyPublicId, boolean lockForUpdate) {
        return validationSupport.resolveLobbyByPublicId(lobbyPublicId, lockForUpdate);
    }

    private void ensureViewerIsMember(GameLobby lobby, UUID learnerId) {
        validationSupport.ensureViewerIsMember(lobby, learnerId);
    }

    private void ensureLobbyStarted(GameLobby lobby) {
        validationSupport.ensureLobbyStarted(lobby);
    }

    private void ensureLobbyNotAbandoned(GameLobby lobby) {
        validationSupport.ensureLobbyNotAbandoned(lobby);
    }

    private void ensurePhase(GameLobby lobby, GameLobbyPhase expectedPhase, String message) {
        validationSupport.ensurePhase(lobby, expectedPhase, message);
    }

    private void ensureViewerIsCurrentDrawer(GameLobby lobby, UUID learnerId) {
        validationSupport.ensureViewerIsCurrentDrawer(lobby, learnerId);
    }

    private PrivateGameLobbyStateDto buildLobbyState(GameLobby lobby, UUID viewerLearnerId) {
        List<GameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        return buildLobbyState(lobby, viewerLearnerId, activeMembers);
    }

    private PrivateGameLobbyStateDto buildLobbyState(
            GameLobby lobby,
            UUID viewerLearnerId,
            List<GameLobbyMember> activeMembers
    ) {
        return stateAssembler.buildLobbyState(lobby, viewerLearnerId, activeMembers);
    }

    private PrivateGameLobbyStateDto buildLobbyState(
            GameLobby lobby,
            UUID viewerLearnerId,
            List<GameLobbyMember> activeMembers,
            Map<UUID, Learner> learnersById
    ) {
        return stateAssembler.buildLobbyState(lobby, viewerLearnerId, activeMembers, learnersById);
    }

    private PrivateGameLobbySharedStateDto buildSharedLobbyState(PrivateGameLobbyStateDto state) {
        return stateAssembler.buildSharedLobbyState(state);
    }

    private PrivateGameLobbyViewerStateDto buildViewerLobbyState(PrivateGameLobbyStateDto state) {
        return stateAssembler.buildViewerLobbyState(state);
    }

    private Map<UUID, Learner> loadLearnersByIdForState(List<GameLobbyMember> activeMembers, GameLobby lobby) {
        return stateAssembler.loadLearnersByIdForState(activeMembers, lobby);
    }

    private GameLobbyMember createMembership(GameLobby lobby, UUID learnerId) {
        return lifecycleSupport.createMembership(lobby, learnerId);
    }

    private SupabaseAuthUser requireLearner(SupabaseAuthUser user) {
        return validationSupport.requireLearner(user);
    }

    private void publishRealtimeState(GameLobby lobby, String reason) {
        realtimeSupport.publishRealtimeState(lobby, reason);
    }

    private void publishRealtimeState(
            GameLobby lobby,
            String reason,
            List<GameLobbyMember> activeMembers
    ) {
        realtimeSupport.publishRealtimeState(lobby, reason, activeMembers);
    }

    private String normalizeLobbyCode(String lobbyCode) {
        return validationSupport.normalizeLobbyCode(lobbyCode);
    }

    private void incrementStateVersion(GameLobby lobby) {
        lifecycleSupport.incrementStateVersion(lobby);
    }

    private void validateBaseVersion(Integer requestedBaseVersion, Integer currentVersion) {
        validationSupport.validateBaseVersion(requestedBaseVersion, currentVersion);
    }

    private void validateRange(int value, int min, int max, String fieldName) {
        validationSupport.validateRange(value, min, max, fieldName);
    }

    private boolean isLobbyCodeUniqueViolation(Throwable error) {
        return lifecycleSupport.isLobbyCodeUniqueViolation(error);
    }

    private boolean isMemberUniqueViolation(Throwable error) {
        return lifecycleSupport.isMemberUniqueViolation(error);
    }
}
