package com.example.demo.game.lobby;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.ImposterWeeklyFeaturedConceptService;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyCodeGenerator;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.lobby.ImposterLobbyPhase;
import com.example.demo.game.imposter.realtime.ImposterLobbyRealtimePublisher;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.game.lobby.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.game.lobby.dto.ImposterConceptResolution;
import com.example.demo.game.lobby.dto.ImposterConceptWinnerSide;
import com.example.demo.game.lobby.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.game.lobby.dto.LeavePrivateImposterLobbyResponse;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyConceptResultDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyLeaveResult;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyMemberStateDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyPlayerScoreDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyReconnectStateDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbySharedStateDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyStateDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyViewerStateDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyVoteTallyDto;
import com.example.demo.game.lobby.dto.SubmitImposterGuessRequest;
import com.example.demo.game.lobby.dto.SubmitImposterVoteRequest;
import com.example.demo.game.lobby.dto.UpdatePrivateImposterLobbySettingsRequest;
import com.example.demo.game.lobby.dto.UpsertImposterDrawingSnapshotRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerImposterLobbyService {

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

    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final ImposterLobbyCodeGenerator imposterLobbyCodeGenerator;
    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final ConceptRepository conceptRepository;
    private final LearnerRepository learnerRepository;
    private final ImposterLobbyRealtimePresenceTracker realtimePresenceTracker;
    private final Clock clock;
    private final ImposterLobbySerializationSupport serializationSupport;
    private final ImposterLobbyValidationSupport validationSupport;
    private final ImposterLobbyLifecycleSupport lifecycleSupport;
    private final ImposterLobbyRoundEngine roundEngine;
    private final ImposterLobbyStateAssembler stateAssembler;
    private final ImposterLobbyRealtimeSupport realtimeSupport;
    private final ImposterLobbyGameFlowSupport gameFlowSupport;
    private final ImposterLobbyOperationsSupport operationsSupport;

    public LearnerImposterLobbyService(
            ImposterGameLobbyRepository imposterGameLobbyRepository,
            ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository,
            ImposterLobbyCodeGenerator imposterLobbyCodeGenerator,
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService,
            ConceptRepository conceptRepository,
            LearnerRepository learnerRepository,
            ImposterLobbyRealtimePublisher realtimePublisher,
            ImposterLobbyRealtimePresenceTracker realtimePresenceTracker,
            Clock clock
    ) {
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.imposterLobbyCodeGenerator = imposterLobbyCodeGenerator;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
        this.conceptRepository = conceptRepository;
        this.learnerRepository = learnerRepository;
        this.realtimePresenceTracker = realtimePresenceTracker;
        this.clock = clock;
        this.serializationSupport = new ImposterLobbySerializationSupport();
        this.validationSupport = new ImposterLobbyValidationSupport(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                DRAWING_VERSION_CONFLICT_MESSAGE
        );
        this.lifecycleSupport = new ImposterLobbyLifecycleSupport(
                imposterGameLobbyMemberRepository,
                clock,
                LOBBY_CODE_UNIQUE_CONSTRAINT,
                MEMBER_UNIQUE_CONSTRAINT
        );
        this.roundEngine = new ImposterLobbyRoundEngine(serializationSupport, CONCEPT_RESULT_DURATION_SECONDS);
        this.stateAssembler = new ImposterLobbyStateAssembler(
                learnerRepository,
                realtimePresenceTracker,
                serializationSupport,
                MIN_ACTIVE_MEMBERS_TO_START,
                MIN_TIMER_SECONDS,
                MAX_TIMER_SECONDS,
                DEFAULT_CONCEPT_COUNT,
                DEFAULT_TURN_DURATION_SECONDS,
                DEFAULT_MAX_VOTING_ROUNDS
        );
        this.realtimeSupport = new ImposterLobbyRealtimeSupport(
                imposterGameLobbyMemberRepository,
                stateAssembler,
                realtimePublisher
        );
        this.gameFlowSupport = new ImposterLobbyGameFlowSupport(
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
        this.operationsSupport = new ImposterLobbyOperationsSupport(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                imposterLobbyCodeGenerator,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                validationSupport,
                lifecycleSupport,
                serializationSupport,
                stateAssembler,
                realtimeSupport,
                gameFlowSupport,
                roundEngine,
                realtimePresenceTracker,
                clock,
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
    public PrivateImposterLobbyDto createPrivateLobby(
            SupabaseAuthUser user,
            CreatePrivateImposterLobbyRequest request
    ) {
        return operationsSupport.createPrivateLobby(user, request);
    }

    @Transactional
    public JoinedPrivateImposterLobbyDto joinPrivateLobby(SupabaseAuthUser user, JoinPrivateImposterLobbyRequest request) {
        return operationsSupport.joinPrivateLobby(user, request);
    }

    @Transactional
    public LeavePrivateImposterLobbyResponse leavePrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        return operationsSupport.leavePrivateLobby(user, lobbyPublicId);
    }

    @Transactional
    public PrivateImposterLobbyStateDto updatePrivateLobbySettings(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpdatePrivateImposterLobbySettingsRequest request
    ) {
        return operationsSupport.updatePrivateLobbySettings(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto startPrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        return operationsSupport.startPrivateLobby(user, lobbyPublicId);
    }

    @Transactional
    public PrivateImposterLobbyStateDto getPrivateLobbyState(SupabaseAuthUser user, UUID lobbyPublicId) {
        return operationsSupport.getPrivateLobbyState(user, lobbyPublicId);
    }

    @Transactional
    public PrivateImposterLobbyStateDto upsertDrawingSnapshot(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        return operationsSupport.upsertDrawingSnapshot(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        return operationsSupport.submitDrawingDone(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterVoteRequest request
    ) {
        return operationsSupport.submitVote(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitImposterGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterGuessRequest request
    ) {
        return operationsSupport.submitImposterGuess(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitLiveDrawing(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        return upsertDrawingSnapshot(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitRealtimeVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterVoteRequest request
    ) {
        return submitVote(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitRealtimeGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterGuessRequest request
    ) {
        return submitImposterGuess(user, lobbyPublicId, request);
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitRealtimeDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
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

    private void initializeConceptRuntime(ImposterGameLobby lobby, List<ImposterGameLobbyMember> activeMembers, OffsetDateTime now) {
        gameFlowSupport.initializeConceptRuntime(lobby, activeMembers, now);
    }

    private boolean resolveTimedTransitionsIfNeeded(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        return gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
    }

    private void advanceToNextDrawStepOrVoting(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        gameFlowSupport.advanceToNextDrawStepOrVoting(lobby, activeMembers, now);
    }

    private void finalizeVotingRound(ImposterGameLobby lobby, List<ImposterGameLobbyMember> activeMembers, OffsetDateTime now) {
        gameFlowSupport.finalizeVotingRound(lobby, activeMembers, now);
    }

    private void resolveConceptOutcome(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now,
            ImposterLobbyGameFlowSupport.WinnerSide winnerSide,
            ImposterLobbyGameFlowSupport.ConceptResolution resolution,
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
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            UUID leftLearnerId,
            OffsetDateTime now
    ) {
        gameFlowSupport.handleActiveGameOnMemberLeave(lobby, activeMembers, leftLearnerId, now);
    }

    private void abandonLobby(
            ImposterGameLobby lobby,
            UUID abandonedByLearnerId,
            OffsetDateTime endedAt,
            String endedReason
    ) {
        roundEngine.abandonLobby(lobby, abandonedByLearnerId, endedAt, endedReason);
    }

    private ImposterGameLobby resolveLobbyByPublicId(UUID lobbyPublicId, boolean lockForUpdate) {
        return validationSupport.resolveLobbyByPublicId(lobbyPublicId, lockForUpdate);
    }

    private void ensureViewerIsMember(ImposterGameLobby lobby, UUID learnerId) {
        validationSupport.ensureViewerIsMember(lobby, learnerId);
    }

    private void ensureLobbyStarted(ImposterGameLobby lobby) {
        validationSupport.ensureLobbyStarted(lobby);
    }

    private void ensureLobbyNotAbandoned(ImposterGameLobby lobby) {
        validationSupport.ensureLobbyNotAbandoned(lobby);
    }

    private void ensurePhase(ImposterGameLobby lobby, ImposterLobbyPhase expectedPhase, String message) {
        validationSupport.ensurePhase(lobby, expectedPhase, message);
    }

    private void ensureViewerIsCurrentDrawer(ImposterGameLobby lobby, UUID learnerId) {
        validationSupport.ensureViewerIsCurrentDrawer(lobby, learnerId);
    }

    private PrivateImposterLobbyStateDto buildLobbyState(ImposterGameLobby lobby, UUID viewerLearnerId) {
        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        return buildLobbyState(lobby, viewerLearnerId, activeMembers);
    }

    private PrivateImposterLobbyStateDto buildLobbyState(
            ImposterGameLobby lobby,
            UUID viewerLearnerId,
            List<ImposterGameLobbyMember> activeMembers
    ) {
        return stateAssembler.buildLobbyState(lobby, viewerLearnerId, activeMembers);
    }

    private PrivateImposterLobbyStateDto buildLobbyState(
            ImposterGameLobby lobby,
            UUID viewerLearnerId,
            List<ImposterGameLobbyMember> activeMembers,
            Map<UUID, Learner> learnersById
    ) {
        return stateAssembler.buildLobbyState(lobby, viewerLearnerId, activeMembers, learnersById);
    }

    private PrivateImposterLobbySharedStateDto buildSharedLobbyState(PrivateImposterLobbyStateDto state) {
        return stateAssembler.buildSharedLobbyState(state);
    }

    private PrivateImposterLobbyViewerStateDto buildViewerLobbyState(PrivateImposterLobbyStateDto state) {
        return stateAssembler.buildViewerLobbyState(state);
    }

    private Map<UUID, Learner> loadLearnersByIdForState(List<ImposterGameLobbyMember> activeMembers, ImposterGameLobby lobby) {
        return stateAssembler.loadLearnersByIdForState(activeMembers, lobby);
    }

    private ImposterGameLobbyMember createMembership(ImposterGameLobby lobby, UUID learnerId) {
        return lifecycleSupport.createMembership(lobby, learnerId);
    }

    private SupabaseAuthUser requireLearner(SupabaseAuthUser user) {
        return validationSupport.requireLearner(user);
    }

    private void publishRealtimeState(ImposterGameLobby lobby, String reason) {
        realtimeSupport.publishRealtimeState(lobby, reason);
    }

    private void publishRealtimeState(
            ImposterGameLobby lobby,
            String reason,
            List<ImposterGameLobbyMember> activeMembers
    ) {
        realtimeSupport.publishRealtimeState(lobby, reason, activeMembers);
    }

    private String normalizeLobbyCode(String lobbyCode) {
        return validationSupport.normalizeLobbyCode(lobbyCode);
    }

    private void incrementStateVersion(ImposterGameLobby lobby) {
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
