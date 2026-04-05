package com.example.demo.me.imposter;

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
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.ImposterConceptResolution;
import com.example.demo.me.imposter.dto.ImposterConceptWinnerSide;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.LeavePrivateImposterLobbyResponse;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyConceptResultDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyLeaveResult;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyMemberStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyPlayerScoreDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyReconnectStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbySharedStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyViewerStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyVoteTallyDto;
import com.example.demo.me.imposter.dto.SubmitImposterGuessRequest;
import com.example.demo.me.imposter.dto.SubmitImposterVoteRequest;
import com.example.demo.me.imposter.dto.UpdatePrivateImposterLobbySettingsRequest;
import com.example.demo.me.imposter.dto.UpsertImposterDrawingSnapshotRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
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

    private enum WinnerSide {
        IMPOSTER,
        NON_IMPOSTERS
    }

    private enum ConceptResolution {
        WRONG_ACCUSATION,
        VOTING_TIE_LIMIT,
        IMPOSTER_GUESS_CORRECT,
        IMPOSTER_GUESS_WRONG
    }

    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final ImposterLobbyCodeGenerator imposterLobbyCodeGenerator;
    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final ConceptRepository conceptRepository;
    private final LearnerRepository learnerRepository;
    private final ImposterLobbyRealtimePublisher realtimePublisher;
    private final ImposterLobbyRealtimePresenceTracker realtimePresenceTracker;
    private final Clock clock;

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
        this.realtimePublisher = realtimePublisher;
        this.realtimePresenceTracker = realtimePresenceTracker;
        this.clock = clock;
    }

    @Transactional
    public PrivateImposterLobbyDto createPrivateLobby(
            SupabaseAuthUser user,
            CreatePrivateImposterLobbyRequest request
    ) {
        requireLearner(user);
        if (request == null || request.conceptPoolMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPoolMode is required");
        }

        String pinnedYearMonth = null;
        if (request.conceptPoolMode() == ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            String currentYearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
            ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(currentYearMonth)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Current monthly imposter pack is not configured"
                    ));

            if (imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId()).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Current monthly imposter pack has no concepts"
                );
            }

            pinnedYearMonth = currentYearMonth;
        }

        for (int attempt = 1; attempt <= LOBBY_CODE_MAX_RETRIES; attempt++) {
            ImposterGameLobby lobby = new ImposterGameLobby();
            lobby.setLobbyCode(imposterLobbyCodeGenerator.generate());
            lobby.setHostLearnerId(user.userId());
            lobby.setPrivateLobby(true);
            lobby.setConceptPoolMode(request.conceptPoolMode());
            lobby.setPinnedYearMonth(pinnedYearMonth);
            lobby.setCreatedAt(OffsetDateTime.now(clock));
            applyDefaultSettings(lobby);

            try {
                ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
                createMembership(savedLobby, user.userId());
                return PrivateImposterLobbyDto.from(savedLobby);
            } catch (DataIntegrityViolationException ex) {
                if (!isLobbyCodeUniqueViolation(ex) || attempt == LOBBY_CODE_MAX_RETRIES) {
                    throw ex;
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate lobby code");
    }

    @Transactional
    public JoinedPrivateImposterLobbyDto joinPrivateLobby(SupabaseAuthUser user, JoinPrivateImposterLobbyRequest request) {
        requireLearner(user);
        String lobbyCode = normalizeLobbyCode(request == null ? null : request.lobbyCode());
        if (lobbyCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode is required");
        }

        ImposterGameLobby lobby = imposterGameLobbyRepository.findByLobbyCodeForUpdate(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));

        ImposterGameLobbyMember activeMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .orElse(null);
        if (activeMember != null) {
            return JoinedPrivateImposterLobbyDto.from(lobby, activeMember, true);
        }

        ImposterGameLobbyMember historicalMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerId(lobby.getId(), user.userId())
                .orElse(null);
        if (historicalMember != null) {
            historicalMember.setJoinedAt(OffsetDateTime.now(clock));
            historicalMember.setLeftAt(null);
            ImposterGameLobbyMember rejoinedMember = imposterGameLobbyMemberRepository.saveAndFlush(historicalMember);
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "JOIN_REJOIN");
            return JoinedPrivateImposterLobbyDto.from(savedLobby, rejoinedMember, false);
        }

        try {
            ImposterGameLobbyMember createdMember = createMembership(lobby, user.userId());
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "JOIN");
            return JoinedPrivateImposterLobbyDto.from(savedLobby, createdMember, false);
        } catch (DataIntegrityViolationException ex) {
            if (isMemberUniqueViolation(ex)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Learner already joined this lobby");
            }
            throw ex;
        }
    }

    @Transactional
    public LeavePrivateImposterLobbyResponse leavePrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());

        ImposterGameLobbyMember member = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Learner is not an active member of this lobby"
                ));

        OffsetDateTime now = OffsetDateTime.now(clock);
        member.setLeftAt(now);
        imposterGameLobbyMemberRepository.saveAndFlush(member);
        realtimePresenceTracker.clearLobbyMembership(lobbyPublicId, user.userId());

        List<ImposterGameLobbyMember> remainingActiveMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        boolean leavingHost = user.userId().equals(lobby.getHostLearnerId());
        boolean startedAndActive = lobby.getStartedAt() != null
                && lobby.getCurrentPhase() != ImposterLobbyPhase.MATCH_COMPLETE
                && lobby.getCurrentPhase() != ImposterLobbyPhase.ABANDONED;

        if (startedAndActive) {
            abandonLobby(lobby, user.userId(), now, END_REASON_PLAYER_QUIT);
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
            publishRealtimeState(savedLobby, "ABANDONED_BY_QUIT", remainingActiveMembers);
            return new LeavePrivateImposterLobbyResponse(
                    PrivateImposterLobbyLeaveResult.LEFT_AND_SESSION_ABANDONED,
                    state
            );
        }

        if (lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
            publishRealtimeState(savedLobby, "LEAVE_AFTER_ABANDONED", remainingActiveMembers);
            return new LeavePrivateImposterLobbyResponse(PrivateImposterLobbyLeaveResult.LEFT, state);
        }

        if (remainingActiveMembers.isEmpty()) {
            imposterGameLobbyMemberRepository.delete(member);
            try {
                imposterGameLobbyRepository.deleteById(lobby.getId());
                imposterGameLobbyRepository.flush();
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Unable to close lobby because it is still referenced by active game data",
                        ex
                );
            }
            return new LeavePrivateImposterLobbyResponse(
                    PrivateImposterLobbyLeaveResult.LEFT_AND_LOBBY_DELETED,
                    null
            );
        }

        if (leavingHost) {
            UUID nextHostLearnerId = remainingActiveMembers.get(0).getLearnerId();
            lobby.setHostLearnerId(nextHostLearnerId);
        }

        handleActiveGameOnMemberLeave(lobby, remainingActiveMembers, member.getLearnerId(), now);
        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);

        PrivateImposterLobbyLeaveResult result = leavingHost
                ? PrivateImposterLobbyLeaveResult.LEFT_AND_PROMOTED_HOST
                : PrivateImposterLobbyLeaveResult.LEFT;
        PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
        publishRealtimeState(savedLobby, leavingHost ? "LEAVE_HOST_TRANSFER" : "LEAVE");
        return new LeavePrivateImposterLobbyResponse(result, state);
    }

    @Transactional
    public PrivateImposterLobbyStateDto updatePrivateLobbySettings(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpdatePrivateImposterLobbySettingsRequest request
    ) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());

        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can update lobby settings");
        }
        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby settings are locked after start");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settings payload is required");
        }

        if (request.conceptCount() != null) {
            validateRange(request.conceptCount(), 1, MAX_CONCEPT_COUNT, "conceptCount");
            lobby.setConceptCount(request.conceptCount());
        }
        if (request.roundsPerConcept() != null) {
            validateRange(request.roundsPerConcept(), 1, MAX_ROUNDS_PER_CONCEPT, "roundsPerConcept");
            lobby.setRoundsPerConcept(request.roundsPerConcept());
        }
        if (request.discussionTimerSeconds() != null) {
            validateRange(request.discussionTimerSeconds(), MIN_TIMER_SECONDS, MAX_TIMER_SECONDS, "discussionTimerSeconds");
            lobby.setDiscussionTimerSeconds(request.discussionTimerSeconds());
        }
        if (request.imposterGuessTimerSeconds() != null) {
            validateRange(request.imposterGuessTimerSeconds(), MIN_TIMER_SECONDS, MAX_TIMER_SECONDS, "imposterGuessTimerSeconds");
            lobby.setImposterGuessTimerSeconds(request.imposterGuessTimerSeconds());
        }
        if (request.turnDurationSeconds() != null) {
            validateRange(request.turnDurationSeconds(), MIN_TIMER_SECONDS, MAX_TIMER_SECONDS, "turnDurationSeconds");
            lobby.setTurnDurationSeconds(request.turnDurationSeconds());
        }

        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId());
        publishRealtimeState(savedLobby, "SETTINGS");
        return state;
    }

    @Transactional
    public PrivateImposterLobbyStateDto startPrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());

        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can start this imposter lobby");
        }

        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby has already started");
        }

        boolean hostIsActiveMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .isPresent();
        if (!hostIsActiveMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby host must be an active member to start");
        }

        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.size() < MIN_ACTIVE_MEMBERS_TO_START) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "At least " + MIN_ACTIVE_MEMBERS_TO_START + " active players are required to start"
            );
        }

        applyDefaultSettings(lobby);
        OffsetDateTime now = OffsetDateTime.now(clock);
        lobby.setStartedAt(now);
        lobby.setStartedByLearnerId(user.userId());
        lobby.setCurrentConceptIndex(1);
        lobby.setUsedConceptPublicIds(null);
        lobby.setPlayerScores(serializeScoreMap(initializeScoreMap(activeMembers)));
        initializeConceptRuntime(lobby, activeMembers, now);

        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), activeMembers);
        publishRealtimeState(savedLobby, "START");
        return state;
    }

    @Transactional
    public PrivateImposterLobbyStateDto getPrivateLobbyState(SupabaseAuthUser user, UUID lobbyPublicId) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());

        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        boolean transitioned = resolveTimedTransitionsIfNeeded(lobby, activeMembers, OffsetDateTime.now(clock));
        if (transitioned) {
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return buildLobbyState(savedLobby, user.userId(), activeMembers);
        }

        return buildLobbyState(lobby, user.userId(), activeMembers);
    }

    @Transactional
    public PrivateImposterLobbyStateDto upsertDrawingSnapshot(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        requireLearner(user);
        if (request == null || request.snapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot is required");
        }

        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());
        ensureLobbyNotAbandoned(lobby);
        ensureLobbyStarted(lobby);
        ensurePhase(lobby, ImposterLobbyPhase.DRAWING, "Drawing is not active");
        ensureViewerIsCurrentDrawer(lobby, user.userId());
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Live drawing is disabled; submit drawing with done");
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        requireLearner(user);
        if (request == null || request.snapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot is required");
        }

        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());
        ensureLobbyNotAbandoned(lobby);
        ensureLobbyStarted(lobby);
        ensurePhase(lobby, ImposterLobbyPhase.DRAWING, "Drawing is not active");
        ensureViewerIsCurrentDrawer(lobby, user.userId());

        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "TURN_EXPIRED");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Drawing turn has ended");
        }

        Integer currentVersion = defaultVersion(lobby);
        validateBaseVersion(request.baseVersion(), currentVersion);
        lobby.setCurrentDrawingSnapshot(request.snapshot());
        lobby.setDrawingVersion(currentVersion + 1);
        lobby.setTurnCompletedAt(now);
        advanceToNextDrawStepOrVoting(lobby, activeMembers, now);

        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), activeMembers);
        publishRealtimeState(savedLobby, "DRAWING_DONE");
        return state;
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterVoteRequest request
    ) {
        requireLearner(user);
        if (request == null || request.suspectedLearnerPublicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "suspectedLearnerPublicId is required");
        }

        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());
        ensureLobbyNotAbandoned(lobby);
        ensureLobbyStarted(lobby);

        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned && lobby.getCurrentPhase() != ImposterLobbyPhase.VOTING) {
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        ensurePhase(lobby, ImposterLobbyPhase.VOTING, "Voting is not active");

        if (lobby.getVotingDeadlineAt() != null && !now.isBefore(lobby.getVotingDeadlineAt())) {
            finalizeVotingRound(lobby, activeMembers, now);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting window has ended");
        }

        Map<UUID, Learner> learnersById = loadLearnersByIdForState(activeMembers, lobby);
        Map<UUID, UUID> learnerIdByPublicId = learnersById.values().stream()
                .collect(Collectors.toMap(Learner::getPublicId, Learner::getId));
        UUID voteTargetLearnerId = learnerIdByPublicId.get(request.suspectedLearnerPublicId());
        if (voteTargetLearnerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vote target");
        }
        if (voteTargetLearnerId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot vote for yourself");
        }

        Set<UUID> eligibleTargets = new LinkedHashSet<>(deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
        if (!eligibleTargets.contains(voteTargetLearnerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vote target is not eligible in this voting round");
        }

        Map<UUID, UUID> ballots = deserializeVoteBallots(lobby.getVotingBallots());
        ballots.put(user.userId(), voteTargetLearnerId);
        lobby.setVotingBallots(serializeVoteBallots(ballots));

        if (ballots.keySet().containsAll(activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList())) {
            finalizeVotingRound(lobby, activeMembers, now);
        }

        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), activeMembers);
        publishRealtimeState(savedLobby, "VOTE");
        return state;
    }

    @Transactional
    public PrivateImposterLobbyStateDto submitImposterGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterGuessRequest request
    ) {
        requireLearner(user);
        if (request == null || request.guess() == null || request.guess().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guess is required");
        }

        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        ensureViewerIsMember(lobby, user.userId());
        ensureLobbyNotAbandoned(lobby);
        ensureLobbyStarted(lobby);

        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned && lobby.getCurrentPhase() != ImposterLobbyPhase.IMPOSTER_GUESS) {
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        ensurePhase(lobby, ImposterLobbyPhase.IMPOSTER_GUESS, "Imposter guess is not active");

        if (!user.userId().equals(lobby.getCurrentImposterLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only current imposter can submit guess");
        }

        if (lobby.getImposterGuessDeadlineAt() != null && !now.isBefore(lobby.getImposterGuessDeadlineAt())) {
            resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    WinnerSide.NON_IMPOSTERS,
                    ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter guess window has ended");
        }

        String guessed = request.guess().trim();
        boolean correct = guessed.equalsIgnoreCase(lobby.getCurrentConceptTitle() == null ? "" : lobby.getCurrentConceptTitle().trim());
        lobby.setLastImposterGuess(guessed);
        lobby.setLastImposterGuessCorrect(correct);
        resolveConceptOutcome(
                lobby,
                activeMembers,
                now,
                correct ? WinnerSide.IMPOSTER : WinnerSide.NON_IMPOSTERS,
                correct ? ConceptResolution.IMPOSTER_GUESS_CORRECT : ConceptResolution.IMPOSTER_GUESS_WRONG,
                lobby.getVotedOutLearnerId(),
                guessed,
                false,
                deserializeVoteTallies(lobby.getLatestResultVoteTallies())
        );

        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = buildLobbyState(savedLobby, user.userId(), activeMembers);
        publishRealtimeState(savedLobby, "GUESS");
        return state;
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
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<UUID> startedLobbyPublicIds = imposterGameLobbyRepository.findByStartedAtIsNotNull()
                .stream()
                .map(ImposterGameLobby::getPublicId)
                .toList();
        for (UUID lobbyPublicId : startedLobbyPublicIds) {
            processTimedTransitionsForLobby(lobbyPublicId, now);
        }
    }

    @Transactional
    public void processRealtimeDisconnectTimeouts() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> dueDisconnects =
                realtimePresenceTracker.consumeDueDisconnectTimeouts(now);
        for (ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate dueDisconnect : dueDisconnects) {
            processDisconnectTimeout(dueDisconnect, now);
        }
    }

    @Transactional
    public void publishRealtimePresenceUpdate(UUID lobbyPublicId, String reason) {
        if (lobbyPublicId == null) {
            return;
        }
        ImposterGameLobby lobby = imposterGameLobbyRepository.findByPublicId(lobbyPublicId).orElse(null);
        if (lobby == null) {
            return;
        }
        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        publishRealtimeState(lobby, reason, activeMembers);
    }

    private void processTimedTransitionsForLobby(UUID lobbyPublicId, OffsetDateTime now) {
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId, true);
        if (lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            return;
        }
        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }

        boolean transitioned = resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
            publishRealtimeState(savedLobby, "DEADLINE_ADVANCE", activeMembers);
        }
    }

    private void processDisconnectTimeout(
            ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate dueDisconnect,
            OffsetDateTime now
    ) {
        ImposterGameLobby lobby;
        try {
            lobby = resolveLobbyByPublicId(dueDisconnect.lobbyPublicId(), true);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return;
            }
            throw ex;
        }
        if (lobby.getStartedAt() == null
                || lobby.getCurrentPhase() == ImposterLobbyPhase.MATCH_COMPLETE
                || lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            return;
        }

        boolean learnerStillActive = imposterGameLobbyMemberRepository
                .existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), dueDisconnect.learnerId());
        if (!learnerStillActive) {
            return;
        }

        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }

        abandonLobby(lobby, dueDisconnect.learnerId(), now, END_REASON_PLAYER_DISCONNECTED_TIMEOUT);
        incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        publishRealtimeState(savedLobby, "ABANDONED_BY_DISCONNECT_TIMEOUT", activeMembers);
    }

    private void applyDefaultSettings(ImposterGameLobby lobby) {
        if (lobby.getConceptCount() == null) {
            lobby.setConceptCount(DEFAULT_CONCEPT_COUNT);
        }
        if (lobby.getRoundsPerConcept() == null) {
            lobby.setRoundsPerConcept(DEFAULT_ROUNDS_PER_CONCEPT);
        }
        if (lobby.getDiscussionTimerSeconds() == null) {
            lobby.setDiscussionTimerSeconds(DEFAULT_DISCUSSION_TIMER_SECONDS);
        }
        if (lobby.getImposterGuessTimerSeconds() == null) {
            lobby.setImposterGuessTimerSeconds(DEFAULT_IMPOSTER_GUESS_TIMER_SECONDS);
        }
        if (lobby.getTurnDurationSeconds() == null) {
            lobby.setTurnDurationSeconds(DEFAULT_TURN_DURATION_SECONDS);
        }
        if (lobby.getMaxVotingRounds() == null) {
            lobby.setMaxVotingRounds(DEFAULT_MAX_VOTING_ROUNDS);
        }
        if (lobby.getStateVersion() == null) {
            lobby.setStateVersion(0);
        }
    }

    private void initializeConceptRuntime(ImposterGameLobby lobby, List<ImposterGameLobbyMember> activeMembers, OffsetDateTime now) {
        applyDefaultSettings(lobby);

        Concept selectedConcept = selectConceptForNextConceptSlot(lobby);
        lobby.setCurrentConceptPublicId(selectedConcept.getPublicId());
        lobby.setCurrentConceptTitle(selectedConcept.getTitle());

        Set<UUID> usedConceptPublicIds = new LinkedHashSet<>(deserializeUuidList(lobby.getUsedConceptPublicIds()));
        usedConceptPublicIds.add(selectedConcept.getPublicId());
        lobby.setUsedConceptPublicIds(serializeUuidList(new ArrayList<>(usedConceptPublicIds)));

        List<UUID> activeLearnerIds = activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList();
        UUID imposterLearnerId = activeLearnerIds.get(ThreadLocalRandom.current().nextInt(activeLearnerIds.size()));
        lobby.setCurrentImposterLearnerId(imposterLearnerId);

        List<UUID> shuffledOrder = new ArrayList<>(activeLearnerIds);
        Collections.shuffle(shuffledOrder);

        lobby.setCurrentPhase(ImposterLobbyPhase.DRAWING);
        lobby.setRoundNumber(1);
        lobby.setRoundDrawerOrder(serializeDrawerOrder(shuffledOrder));
        lobby.setCurrentTurnIndex(0);
        lobby.setCurrentDrawerLearnerId(shuffledOrder.get(0));
        lobby.setTurnStartedAt(now);
        lobby.setTurnEndsAt(now.plusSeconds(defaultTurnDuration(lobby)));
        lobby.setTurnCompletedAt(null);
        lobby.setRoundCompletedAt(null);

        lobby.setCurrentDrawingSnapshot(null);
        lobby.setDrawingVersion(0);

        lobby.setVotingRoundNumber(null);
        lobby.setVotingEligibleTargetLearnerIds(null);
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(null);
        lobby.setVotedOutLearnerId(null);
        lobby.setImposterGuessDeadlineAt(null);
        lobby.setLastImposterGuess(null);
        lobby.setLastImposterGuessCorrect(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    private Concept selectConceptForNextConceptSlot(ImposterGameLobby lobby) {
        Set<UUID> excluded = new LinkedHashSet<>(deserializeUuidList(lobby.getUsedConceptPublicIds()));

        if (lobby.getConceptPoolMode() == ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            if (lobby.getPinnedYearMonth() == null || lobby.getPinnedYearMonth().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby monthly pack is not configured");
            }

            var featured = imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept(lobby.getPinnedYearMonth());
            if (featured.isPresent() && !excluded.contains(featured.get().getPublicId())) {
                return featured.get();
            }

            ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(lobby.getPinnedYearMonth())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Pinned monthly imposter pack is not configured: " + lobby.getPinnedYearMonth()
                    ));

            List<Concept> monthlyConcepts = imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId())
                    .stream()
                    .map(row -> row.getConcept())
                    .filter(concept -> !excluded.contains(concept.getPublicId()))
                    .toList();
            if (monthlyConcepts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No imposter game concepts are available");
            }
            return monthlyConcepts.get(ThreadLocalRandom.current().nextInt(monthlyConcepts.size()));
        }

        List<Concept> concepts = conceptRepository.findAll()
                .stream()
                .filter(concept -> !excluded.contains(concept.getPublicId()))
                .toList();
        if (concepts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No imposter game concepts are available");
        }
        return concepts.get(ThreadLocalRandom.current().nextInt(concepts.size()));
    }

    private boolean resolveTimedTransitionsIfNeeded(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        boolean transitioned = false;
        if (lobby.getCurrentPhase() == ImposterLobbyPhase.VOTING
                && lobby.getVotingDeadlineAt() != null
                && !now.isBefore(lobby.getVotingDeadlineAt())) {
            finalizeVotingRound(lobby, activeMembers, now);
            transitioned = true;
        }

        if (lobby.getCurrentPhase() == ImposterLobbyPhase.DRAWING
                && lobby.getTurnEndsAt() != null
                && !now.isBefore(lobby.getTurnEndsAt())) {
            lobby.setTurnCompletedAt(now);
            advanceToNextDrawStepOrVoting(lobby, activeMembers, now);
            transitioned = true;
        }

        if (lobby.getCurrentPhase() == ImposterLobbyPhase.IMPOSTER_GUESS
                && lobby.getImposterGuessDeadlineAt() != null
                && !now.isBefore(lobby.getImposterGuessDeadlineAt())) {
            resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    WinnerSide.NON_IMPOSTERS,
                    ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
            transitioned = true;
        }

        if (lobby.getCurrentPhase() == ImposterLobbyPhase.CONCEPT_RESULT
                && lobby.getConceptResultDeadlineAt() != null
                && !now.isBefore(lobby.getConceptResultDeadlineAt())) {
            advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now);
            transitioned = true;
        }
        return transitioned;
    }

    private void advanceToNextDrawStepOrVoting(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        List<UUID> drawerOrder = deserializeDrawerOrder(lobby.getRoundDrawerOrder());
        if (drawerOrder.isEmpty()) {
            startVotingPhase(lobby, activeMembers, now);
            return;
        }

        Set<UUID> activeLearnerIds = activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).collect(Collectors.toSet());
        int currentIndex = lobby.getCurrentTurnIndex() == null ? -1 : lobby.getCurrentTurnIndex();
        for (int nextIndex = currentIndex + 1; nextIndex < drawerOrder.size(); nextIndex++) {
            UUID nextDrawerLearnerId = drawerOrder.get(nextIndex);
            if (!activeLearnerIds.contains(nextDrawerLearnerId)) {
                continue;
            }

            lobby.setCurrentTurnIndex(nextIndex);
            lobby.setCurrentDrawerLearnerId(nextDrawerLearnerId);
            lobby.setTurnStartedAt(now);
            lobby.setTurnEndsAt(now.plusSeconds(defaultTurnDuration(lobby)));
            lobby.setTurnCompletedAt(null);
            lobby.setRoundCompletedAt(null);
            return;
        }

        int roundsPerConcept = lobby.getRoundsPerConcept() == null ? DEFAULT_ROUNDS_PER_CONCEPT : lobby.getRoundsPerConcept();
        int currentRound = lobby.getRoundNumber() == null ? 1 : lobby.getRoundNumber();
        if (currentRound < roundsPerConcept) {
            lobby.setRoundNumber(currentRound + 1);
            for (int nextIndex = 0; nextIndex < drawerOrder.size(); nextIndex++) {
                UUID nextDrawerLearnerId = drawerOrder.get(nextIndex);
                if (!activeLearnerIds.contains(nextDrawerLearnerId)) {
                    continue;
                }
                lobby.setCurrentTurnIndex(nextIndex);
                lobby.setCurrentDrawerLearnerId(nextDrawerLearnerId);
                lobby.setTurnStartedAt(now);
                lobby.setTurnEndsAt(now.plusSeconds(defaultTurnDuration(lobby)));
                lobby.setTurnCompletedAt(null);
                lobby.setRoundCompletedAt(null);
                return;
            }
        }

        startVotingPhase(lobby, activeMembers, now);
    }

    private void startVotingPhase(ImposterGameLobby lobby, List<ImposterGameLobbyMember> activeMembers, OffsetDateTime now) {
        List<UUID> eligibleTargets = activeMembers.stream()
                .sorted(Comparator.comparing(ImposterGameLobbyMember::getJoinedAt))
                .map(ImposterGameLobbyMember::getLearnerId)
                .toList();
        if (eligibleTargets.isEmpty()) {
            advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now);
            return;
        }

        lobby.setCurrentPhase(ImposterLobbyPhase.VOTING);
        lobby.setCurrentDrawerLearnerId(null);
        lobby.setCurrentTurnIndex(null);
        lobby.setTurnStartedAt(null);
        lobby.setTurnEndsAt(null);
        lobby.setTurnCompletedAt(now);
        lobby.setRoundCompletedAt(now);

        lobby.setVotingRoundNumber(1);
        lobby.setVotingEligibleTargetLearnerIds(serializeUuidList(eligibleTargets));
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(now.plusSeconds(defaultDiscussionTimer(lobby)));
        lobby.setVotedOutLearnerId(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    private void finalizeVotingRound(ImposterGameLobby lobby, List<ImposterGameLobbyMember> activeMembers, OffsetDateTime now) {
        Set<UUID> activeLearnerIds = activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).collect(Collectors.toSet());
        List<UUID> eligibleTargets = new ArrayList<>(deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
        eligibleTargets.removeIf(id -> !activeLearnerIds.contains(id));

        if (eligibleTargets.isEmpty()) {
            advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now);
            return;
        }

        if (eligibleTargets.size() == 1) {
            onVoteWinner(
                    lobby,
                    activeMembers,
                    eligibleTargets.get(0),
                    now,
                    buildVoteTallyFromBallots(lobby.getVotingBallots(), eligibleTargets)
            );
            return;
        }

        Map<UUID, UUID> ballots = deserializeVoteBallots(lobby.getVotingBallots());
        ballots.entrySet().removeIf(entry -> !activeLearnerIds.contains(entry.getKey()) || !eligibleTargets.contains(entry.getValue()));
        lobby.setVotingBallots(serializeVoteBallots(ballots));

        Map<UUID, Integer> tally = new HashMap<>();
        for (UUID target : ballots.values()) {
            tally.put(target, tally.getOrDefault(target, 0) + 1);
        }
        for (UUID eligibleTarget : eligibleTargets) {
            tally.putIfAbsent(eligibleTarget, 0);
        }
        lobby.setLatestResultVoteTallies(serializeVoteTallies(tally));

        List<UUID> tiedTargets;
        if (tally.isEmpty()) {
            tiedTargets = new ArrayList<>(eligibleTargets);
        } else {
            int maxVotes = tally.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            tiedTargets = tally.entrySet().stream()
                    .filter(entry -> entry.getValue() == maxVotes)
                    .map(Map.Entry::getKey)
                    .toList();
        }

        if (tiedTargets.size() > 1) {
            int currentRound = lobby.getVotingRoundNumber() == null ? 1 : lobby.getVotingRoundNumber();
            if (currentRound >= defaultMaxVotingRounds(lobby)) {
                resolveConceptOutcome(
                        lobby,
                        activeMembers,
                        now,
                        WinnerSide.IMPOSTER,
                        ConceptResolution.VOTING_TIE_LIMIT,
                        null,
                        null,
                        true,
                        tally
                );
                return;
            }
            lobby.setCurrentPhase(ImposterLobbyPhase.VOTING);
            lobby.setVotingRoundNumber(currentRound + 1);
            lobby.setVotingEligibleTargetLearnerIds(serializeUuidList(tiedTargets));
            lobby.setVotingBallots(null);
            lobby.setVotingDeadlineAt(now.plusSeconds(defaultDiscussionTimer(lobby)));
            lobby.setVotedOutLearnerId(null);
            return;
        }

        onVoteWinner(lobby, activeMembers, tiedTargets.get(0), now, tally);
    }

    private void onVoteWinner(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            UUID votedOutLearnerId,
            OffsetDateTime now,
            Map<UUID, Integer> voteTallies
    ) {
        lobby.setVotedOutLearnerId(votedOutLearnerId);

        if (!votedOutLearnerId.equals(lobby.getCurrentImposterLearnerId())) {
            resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    WinnerSide.IMPOSTER,
                    ConceptResolution.WRONG_ACCUSATION,
                    votedOutLearnerId,
                    null,
                    false,
                    voteTallies
            );
            return;
        }

        if (votedOutLearnerId.equals(lobby.getCurrentImposterLearnerId())) {
            lobby.setCurrentPhase(ImposterLobbyPhase.IMPOSTER_GUESS);
            lobby.setImposterGuessDeadlineAt(now.plusSeconds(defaultImposterGuessTimer(lobby)));
            lobby.setTurnEndsAt(null);
            return;
        }
    }

    private void resolveConceptOutcome(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now,
            WinnerSide winnerSide,
            ConceptResolution resolution,
            UUID accusedLearnerId,
            String imposterGuess,
            boolean imposterWinsByVotingTie,
            Map<UUID, Integer> voteTallies
    ) {
        Map<UUID, Integer> scores = deserializeScoreMap(lobby.getPlayerScores());
        for (ImposterGameLobbyMember activeMember : activeMembers) {
            scores.putIfAbsent(activeMember.getLearnerId(), 0);
        }

        if (winnerSide == WinnerSide.IMPOSTER) {
            scores.put(
                    lobby.getCurrentImposterLearnerId(),
                    scores.getOrDefault(lobby.getCurrentImposterLearnerId(), 0) + 1
            );
        } else {
            for (ImposterGameLobbyMember activeMember : activeMembers) {
                if (!activeMember.getLearnerId().equals(lobby.getCurrentImposterLearnerId())) {
                    scores.put(activeMember.getLearnerId(), scores.getOrDefault(activeMember.getLearnerId(), 0) + 1);
                }
            }
        }

        lobby.setPlayerScores(serializeScoreMap(scores));
        lobby.setLatestResultConceptNumber(lobby.getCurrentConceptIndex());
        lobby.setLatestResultConceptLabel(lobby.getCurrentConceptTitle());
        lobby.setLatestResultWinnerSide(winnerSide.name());
        lobby.setLatestResultResolution(resolution.name());
        lobby.setLatestResultAccusedLearnerId(accusedLearnerId);
        lobby.setLatestResultImposterLearnerId(lobby.getCurrentImposterLearnerId());
        lobby.setLatestResultImposterWinsByVotingTie(imposterWinsByVotingTie);
        lobby.setLatestResultImposterGuess(imposterGuess);
        lobby.setLatestResultVoteTallies(serializeVoteTallies(voteTallies));

        lobby.setCurrentPhase(ImposterLobbyPhase.CONCEPT_RESULT);
        lobby.setConceptResultDeadlineAt(now.plusSeconds(CONCEPT_RESULT_DURATION_SECONDS));
        lobby.setCurrentDrawerLearnerId(null);
        lobby.setCurrentTurnIndex(null);
        lobby.setTurnStartedAt(null);
        lobby.setTurnEndsAt(null);
        lobby.setTurnCompletedAt(now);
        lobby.setRoundCompletedAt(now);
        lobby.setVotingRoundNumber(null);
        lobby.setVotingEligibleTargetLearnerIds(null);
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(null);
        lobby.setImposterGuessDeadlineAt(null);
    }

    private void advanceToNextConceptOrCompleteMatch(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        int conceptCount = defaultConceptCount(lobby);
        int currentConceptIndex = lobby.getCurrentConceptIndex() == null ? 1 : lobby.getCurrentConceptIndex();

        if (currentConceptIndex < conceptCount) {
            lobby.setCurrentConceptIndex(currentConceptIndex + 1);
            initializeConceptRuntime(lobby, activeMembers, now);
            return;
        }

        lobby.setCurrentPhase(ImposterLobbyPhase.MATCH_COMPLETE);
        lobby.setCurrentDrawerLearnerId(null);
        lobby.setCurrentTurnIndex(null);
        lobby.setTurnStartedAt(null);
        lobby.setTurnEndsAt(null);
        lobby.setTurnCompletedAt(now);
        lobby.setRoundCompletedAt(now);
        lobby.setVotingRoundNumber(null);
        lobby.setVotingEligibleTargetLearnerIds(null);
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(null);
        lobby.setImposterGuessDeadlineAt(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    private void handleActiveGameOnMemberLeave(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            UUID leftLearnerId,
            OffsetDateTime now
    ) {
        if (lobby.getStartedAt() == null || lobby.getCurrentPhase() == ImposterLobbyPhase.MATCH_COMPLETE) {
            return;
        }

        if (leftLearnerId.equals(lobby.getCurrentImposterLearnerId())) {
            lobby.setCurrentImposterLearnerId(activeMembers.get(0).getLearnerId());
        }

        if (lobby.getCurrentPhase() == null || lobby.getCurrentPhase() == ImposterLobbyPhase.DRAWING) {
            advanceCurrentTurnIfDrawerUnavailable(lobby, activeMembers, now);
            return;
        }

        if (lobby.getCurrentPhase() == ImposterLobbyPhase.VOTING) {
            List<UUID> eligible = new ArrayList<>(deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
            Set<UUID> activeLearnerIds = activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).collect(Collectors.toSet());
            eligible.removeIf(id -> !activeLearnerIds.contains(id));
            lobby.setVotingEligibleTargetLearnerIds(serializeUuidList(eligible));

            Map<UUID, UUID> ballots = deserializeVoteBallots(lobby.getVotingBallots());
            ballots.entrySet().removeIf(entry -> !activeLearnerIds.contains(entry.getKey()) || !eligible.contains(entry.getValue()));
            lobby.setVotingBallots(serializeVoteBallots(ballots));

            if (eligible.size() <= 1) {
                if (eligible.isEmpty()) {
                    advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now);
                } else {
                    onVoteWinner(
                            lobby,
                            activeMembers,
                            eligible.get(0),
                            now,
                            buildVoteTallyFromBallots(lobby.getVotingBallots(), eligible)
                    );
                }
                return;
            }

            if (ballots.keySet().containsAll(activeLearnerIds)) {
                finalizeVotingRound(lobby, activeMembers, now);
            }
            return;
        }

        if (lobby.getCurrentPhase() == ImposterLobbyPhase.IMPOSTER_GUESS
                && !activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList().contains(lobby.getCurrentImposterLearnerId())) {
            resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    WinnerSide.NON_IMPOSTERS,
                    ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
        }
    }

    private void abandonLobby(
            ImposterGameLobby lobby,
            UUID abandonedByLearnerId,
            OffsetDateTime endedAt,
            String endedReason
    ) {
        lobby.setCurrentPhase(ImposterLobbyPhase.ABANDONED);
        lobby.setEndedReason(endedReason);
        lobby.setEndedAt(endedAt);
        lobby.setAbandonedByLearnerId(abandonedByLearnerId);
        lobby.setCurrentDrawerLearnerId(null);
        lobby.setCurrentTurnIndex(null);
        lobby.setTurnStartedAt(null);
        lobby.setTurnEndsAt(null);
        lobby.setTurnCompletedAt(endedAt);
        lobby.setRoundCompletedAt(endedAt);
        lobby.setVotingRoundNumber(null);
        lobby.setVotingEligibleTargetLearnerIds(null);
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(null);
        lobby.setImposterGuessDeadlineAt(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    private ImposterGameLobby resolveLobbyByPublicId(UUID lobbyPublicId, boolean lockForUpdate) {
        if (lobbyPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyPublicId is required");
        }

        return (lockForUpdate
                ? imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)
                : imposterGameLobbyRepository.findByPublicId(lobbyPublicId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));
    }

    private void ensureViewerIsMember(ImposterGameLobby lobby, UUID learnerId) {
        if (!imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(lobby.getId(), learnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner is not a member of this lobby");
        }
    }

    private void ensureLobbyStarted(ImposterGameLobby lobby) {
        if (lobby.getStartedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby has not started yet");
        }
    }

    private void ensureLobbyNotAbandoned(ImposterGameLobby lobby) {
        if (lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby session has been abandoned");
        }
    }

    private void ensurePhase(ImposterGameLobby lobby, ImposterLobbyPhase expectedPhase, String message) {
        ImposterLobbyPhase actualPhase = lobby.getCurrentPhase() == null
                ? ImposterLobbyPhase.DRAWING
                : lobby.getCurrentPhase();
        if (actualPhase != expectedPhase) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void ensureViewerIsCurrentDrawer(ImposterGameLobby lobby, UUID learnerId) {
        if (!learnerId.equals(lobby.getCurrentDrawerLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only current drawer can perform this action");
        }
    }

    private void advanceCurrentTurnIfDrawerUnavailable(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        if (lobby.getStartedAt() == null || lobby.getCurrentDrawerLearnerId() == null) {
            return;
        }

        Set<UUID> activeLearnerIds = activeMembers.stream()
                .map(ImposterGameLobbyMember::getLearnerId)
                .collect(Collectors.toSet());
        if (activeLearnerIds.contains(lobby.getCurrentDrawerLearnerId())) {
            return;
        }

        advanceToNextDrawStepOrVoting(lobby, activeMembers, now);
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
        Map<UUID, Learner> learnersById = loadLearnersByIdForState(activeMembers, lobby);
        return buildLobbyState(lobby, viewerLearnerId, activeMembers, learnersById);
    }

    private PrivateImposterLobbyStateDto buildLobbyState(
            ImposterGameLobby lobby,
            UUID viewerLearnerId,
            List<ImposterGameLobbyMember> activeMembers,
            Map<UUID, Learner> learnersById
    ) {
        List<PrivateImposterLobbyMemberStateDto> activeMemberDtos = activeMembers.stream()
                .map(member -> {
                    Learner learner = learnersById.get(member.getLearnerId());
                    return new PrivateImposterLobbyMemberStateDto(
                            learner == null ? null : learner.getPublicId(),
                            learner == null ? null : learner.getUsername(),
                            member.getJoinedAt(),
                            member.getLearnerId().equals(lobby.getHostLearnerId())
                    );
                })
                .toList();

        UUID currentDrawerPublicId = toLearnerPublicId(learnersById, lobby.getCurrentDrawerLearnerId());
        UUID votedOutPublicId = toLearnerPublicId(learnersById, lobby.getVotedOutLearnerId());
        UUID endedByPublicId = toLearnerPublicId(learnersById, lobby.getAbandonedByLearnerId());
        Map<UUID, UUID> ballots = deserializeVoteBallots(lobby.getVotingBallots());
        UUID viewerVoteTargetLearnerId = viewerLearnerId == null ? null : ballots.get(viewerLearnerId);
        UUID viewerVoteTargetPublicId = toLearnerPublicId(learnersById, viewerVoteTargetLearnerId);
        Map<UUID, Integer> playerScoreMap = deserializeScoreMap(lobby.getPlayerScores());
        List<PrivateImposterLobbyPlayerScoreDto> playerScores = playerScoreMap.entrySet().stream()
                .map(entry -> new PrivateImposterLobbyPlayerScoreDto(
                        toLearnerPublicId(learnersById, entry.getKey()),
                        entry.getValue()
                ))
                .filter(entry -> entry.learnerPublicId() != null)
                .sorted(Comparator.comparingInt(PrivateImposterLobbyPlayerScoreDto::points).reversed())
                .toList();

        List<UUID> eligibleVoteTargetPublicIds = deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()).stream()
                .map(targetId -> toLearnerPublicId(learnersById, targetId))
                .filter(id -> id != null)
                .toList();
        List<PrivateImposterLobbyReconnectStateDto> reconnectingLearners = realtimePresenceTracker
                .listReconnectPresence(lobby.getPublicId())
                .stream()
                .map(snapshot -> new PrivateImposterLobbyReconnectStateDto(
                        toLearnerPublicId(learnersById, snapshot.learnerId()),
                        snapshot.disconnectDeadlineAt()
                ))
                .filter(snapshot -> snapshot.learnerPublicId() != null)
                .toList();

        long activeMemberCount = activeMemberDtos.size();
        boolean viewerIsHost = viewerLearnerId != null && viewerLearnerId.equals(lobby.getHostLearnerId());
        boolean viewerIsActiveMember = viewerLearnerId != null
                && activeMembers.stream().anyMatch(member -> member.getLearnerId().equals(viewerLearnerId));
        boolean notStarted = lobby.getStartedAt() == null;
        boolean viewerIsCurrentDrawer = viewerLearnerId != null && viewerLearnerId.equals(lobby.getCurrentDrawerLearnerId());
        Integer totalTurns = deserializeDrawerOrder(lobby.getRoundDrawerOrder()).size();
        if (totalTurns == 0 && lobby.getStartedAt() == null) {
            totalTurns = null;
        }

        boolean drawingActive = lobby.getStartedAt() != null
                && (lobby.getCurrentPhase() == null || lobby.getCurrentPhase() == ImposterLobbyPhase.DRAWING);
        boolean roundComplete = !drawingActive || lobby.getRoundCompletedAt() != null;
        boolean viewerIsImposter = viewerLearnerId != null && viewerLearnerId.equals(lobby.getCurrentImposterLearnerId());
        String viewerConceptTitle = viewerIsImposter ? null : lobby.getCurrentConceptTitle();
        PrivateImposterLobbyConceptResultDto latestConceptResult = buildLatestConceptResult(lobby, learnersById);

        return new PrivateImposterLobbyStateDto(
                lobby.getPublicId(),
                lobby.getLobbyCode(),
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getConceptCount(),
                lobby.getRoundsPerConcept(),
                lobby.getDiscussionTimerSeconds(),
                lobby.getImposterGuessTimerSeconds(),
                lobby.getCreatedAt(),
                lobby.getStartedAt(),
                activeMemberCount,
                activeMemberDtos,
                viewerIsHost,
                viewerIsActiveMember,
                viewerIsActiveMember,
                viewerIsHost && viewerIsActiveMember && notStarted && activeMemberCount >= MIN_ACTIVE_MEMBERS_TO_START,
                currentDrawerPublicId,
                lobby.getCurrentTurnIndex(),
                totalTurns,
                defaultTurnDuration(lobby),
                lobby.getTurnStartedAt(),
                lobby.getTurnEndsAt(),
                viewerIsCurrentDrawer,
                drawingActive && viewerIsCurrentDrawer && !roundComplete,
                drawingActive && viewerIsCurrentDrawer && !roundComplete,
                roundComplete,
                lobby.getCurrentDrawingSnapshot(),
                defaultVersion(lobby),
                lobby.getCurrentPhase(),
                lobby.getEndedReason(),
                lobby.getEndedAt(),
                endedByPublicId,
                reconnectingLearners,
                lobby.getCurrentConceptIndex(),
                defaultConceptCount(lobby),
                playerScores,
                latestConceptResult,
                defaultMaxVotingRounds(lobby),
                MIN_TIMER_SECONDS,
                MAX_TIMER_SECONDS,
                defaultStateVersion(lobby),
                lobby.getConceptResultDeadlineAt(),
                viewerVoteTargetPublicId,
                eligibleVoteTargetPublicIds,
                lobby.getVotingRoundNumber(),
                lobby.getVotingDeadlineAt(),
                votedOutPublicId,
                lobby.getImposterGuessDeadlineAt(),
                viewerIsImposter,
                viewerConceptTitle,
                lobby.getLastImposterGuess(),
                lobby.getLastImposterGuessCorrect()
        );
    }

    private PrivateImposterLobbySharedStateDto buildSharedLobbyState(PrivateImposterLobbyStateDto state) {
        return new PrivateImposterLobbySharedStateDto(
                state.publicId(),
                state.lobbyCode(),
                state.isPrivate(),
                state.conceptPoolMode(),
                state.pinnedYearMonth(),
                state.conceptCount(),
                state.roundsPerConcept(),
                state.discussionTimerSeconds(),
                state.imposterGuessTimerSeconds(),
                state.createdAt(),
                state.startedAt(),
                state.activeMemberCount(),
                state.activeMembers(),
                state.currentDrawerPublicId(),
                state.currentTurnIndex(),
                state.totalTurns(),
                state.turnDurationSeconds(),
                state.turnStartedAt(),
                state.turnEndsAt(),
                state.isRoundComplete(),
                state.currentDrawingSnapshot(),
                state.drawingVersion(),
                state.currentPhase(),
                state.endReason(),
                state.endedAt(),
                state.endedByPublicId(),
                state.reconnectingLearners(),
                state.currentConceptIndex(),
                state.totalConcepts(),
                state.playerScores(),
                state.latestConceptResult(),
                state.maxVotingRounds(),
                state.minTimerSeconds(),
                state.maxTimerSeconds(),
                state.stateVersion(),
                state.conceptResultDeadlineAt(),
                state.eligibleVoteTargetPublicIds(),
                state.votingRoundNumber(),
                state.votingDeadlineAt(),
                state.votedOutPublicId(),
                state.imposterGuessDeadlineAt(),
                state.lastImposterGuess(),
                state.lastImposterGuessCorrect()
        );
    }

    private PrivateImposterLobbyViewerStateDto buildViewerLobbyState(PrivateImposterLobbyStateDto state) {
        return new PrivateImposterLobbyViewerStateDto(
                state.viewerVoteTargetPublicId(),
                state.viewerIsImposter(),
                state.viewerConceptTitle()
        );
    }

    private Map<UUID, Learner> loadLearnersByIdForState(List<ImposterGameLobbyMember> activeMembers, ImposterGameLobby lobby) {
        Set<UUID> learnerIds = new LinkedHashSet<>(activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList());
        learnerIds.addAll(deserializeScoreMap(lobby.getPlayerScores()).keySet());
        learnerIds.addAll(deserializeVoteTallies(lobby.getLatestResultVoteTallies()).keySet());
        learnerIds.addAll(deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
        learnerIds.add(lobby.getCurrentDrawerLearnerId());
        learnerIds.add(lobby.getVotedOutLearnerId());
        learnerIds.add(lobby.getCurrentImposterLearnerId());
        learnerIds.add(lobby.getLatestResultAccusedLearnerId());
        learnerIds.add(lobby.getLatestResultImposterLearnerId());
        learnerIds.add(lobby.getAbandonedByLearnerId());
        learnerIds.remove(null);

        List<UUID> orderedLearnerIds = new ArrayList<>(learnerIds);
        return learnerRepository.findAllById(orderedLearnerIds)
                .stream()
                .collect(Collectors.toMap(Learner::getId, Function.identity()));
    }

    private UUID toLearnerPublicId(Map<UUID, Learner> learnersById, UUID learnerId) {
        if (learnerId == null) {
            return null;
        }
        Learner learner = learnersById.get(learnerId);
        return learner == null ? null : learner.getPublicId();
    }

    private PrivateImposterLobbyConceptResultDto buildLatestConceptResult(
            ImposterGameLobby lobby,
            Map<UUID, Learner> learnersById
    ) {
        if (lobby.getLatestResultConceptNumber() == null || lobby.getLatestResultWinnerSide() == null || lobby.getLatestResultResolution() == null) {
            return null;
        }

        ImposterConceptWinnerSide winnerSide = parseWinnerSide(lobby.getLatestResultWinnerSide());
        ImposterConceptResolution resolution = parseResolution(lobby.getLatestResultResolution());
        if (winnerSide == null || resolution == null) {
            return null;
        }

        List<PrivateImposterLobbyVoteTallyDto> tallies = deserializeVoteTallies(lobby.getLatestResultVoteTallies()).entrySet()
                .stream()
                .map(entry -> new PrivateImposterLobbyVoteTallyDto(
                        toLearnerPublicId(learnersById, entry.getKey()),
                        entry.getValue()
                ))
                .filter(entry -> entry.learnerPublicId() != null)
                .sorted(Comparator.comparingInt(PrivateImposterLobbyVoteTallyDto::voteCount).reversed())
                .toList();

        return new PrivateImposterLobbyConceptResultDto(
                lobby.getLatestResultConceptNumber(),
                lobby.getLatestResultConceptLabel(),
                winnerSide,
                resolution,
                toLearnerPublicId(learnersById, lobby.getLatestResultAccusedLearnerId()),
                toLearnerPublicId(learnersById, lobby.getLatestResultImposterLearnerId()),
                Boolean.TRUE.equals(lobby.getLatestResultImposterWinsByVotingTie()),
                lobby.getLatestResultImposterGuess(),
                tallies
        );
    }

    private ImposterConceptWinnerSide parseWinnerSide(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ImposterConceptWinnerSide.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ImposterConceptResolution parseResolution(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ImposterConceptResolution.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ImposterGameLobbyMember createMembership(ImposterGameLobby lobby, UUID learnerId) {
        ImposterGameLobbyMember member = new ImposterGameLobbyMember();
        member.setLobby(lobby);
        member.setLearnerId(learnerId);
        member.setJoinedAt(OffsetDateTime.now(clock));
        return imposterGameLobbyMemberRepository.saveAndFlush(member);
    }

    private SupabaseAuthUser requireLearner(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user;
    }

    private void publishRealtimeState(ImposterGameLobby lobby, String reason) {
        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        publishRealtimeState(lobby, reason, activeMembers);
    }

    private void publishRealtimeState(
            ImposterGameLobby lobby,
            String reason,
            List<ImposterGameLobbyMember> activeMembers
    ) {
        Map<UUID, Learner> learnersById = loadLearnersByIdForState(activeMembers, lobby);
        PrivateImposterLobbyStateDto sharedBase = buildLobbyState(lobby, null, activeMembers, learnersById);
        realtimePublisher.publishSharedLobbyState(
                lobby.getPublicId(),
                defaultStateVersion(lobby),
                reason,
                buildSharedLobbyState(sharedBase)
        );

        for (ImposterGameLobbyMember activeMember : activeMembers) {
            PrivateImposterLobbyStateDto viewerState = buildLobbyState(
                    lobby,
                    activeMember.getLearnerId(),
                    activeMembers,
                    learnersById
            );
            realtimePublisher.publishViewerLobbyState(
                    lobby.getPublicId(),
                    activeMember.getLearnerId(),
                    defaultStateVersion(lobby),
                    reason,
                    buildViewerLobbyState(viewerState)
            );
        }
    }

    private String normalizeLobbyCode(String lobbyCode) {
        if (lobbyCode == null) {
            return null;
        }

        String normalized = lobbyCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private int defaultConceptCount(ImposterGameLobby lobby) {
        return lobby.getConceptCount() == null ? DEFAULT_CONCEPT_COUNT : lobby.getConceptCount();
    }

    private int defaultDiscussionTimer(ImposterGameLobby lobby) {
        return lobby.getDiscussionTimerSeconds() == null
                ? DEFAULT_DISCUSSION_TIMER_SECONDS
                : lobby.getDiscussionTimerSeconds();
    }

    private int defaultImposterGuessTimer(ImposterGameLobby lobby) {
        return lobby.getImposterGuessTimerSeconds() == null
                ? DEFAULT_IMPOSTER_GUESS_TIMER_SECONDS
                : lobby.getImposterGuessTimerSeconds();
    }

    private int defaultTurnDuration(ImposterGameLobby lobby) {
        return lobby.getTurnDurationSeconds() == null
                ? DEFAULT_TURN_DURATION_SECONDS
                : lobby.getTurnDurationSeconds();
    }

    private int defaultMaxVotingRounds(ImposterGameLobby lobby) {
        return lobby.getMaxVotingRounds() == null ? DEFAULT_MAX_VOTING_ROUNDS : lobby.getMaxVotingRounds();
    }

    private Integer defaultVersion(ImposterGameLobby lobby) {
        return lobby.getDrawingVersion() == null ? 0 : lobby.getDrawingVersion();
    }

    private Integer defaultStateVersion(ImposterGameLobby lobby) {
        return lobby.getStateVersion() == null ? 0 : lobby.getStateVersion();
    }

    private void incrementStateVersion(ImposterGameLobby lobby) {
        lobby.setStateVersion(defaultStateVersion(lobby) + 1);
    }

    private void validateBaseVersion(Integer requestedBaseVersion, Integer currentVersion) {
        if (requestedBaseVersion != null && !requestedBaseVersion.equals(currentVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DRAWING_VERSION_CONFLICT_MESSAGE);
        }
    }

    private void validateRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be between " + min + " and " + max);
        }
    }

    private String serializeDrawerOrder(List<UUID> drawerOrder) {
        return serializeUuidList(drawerOrder);
    }

    private List<UUID> deserializeDrawerOrder(String serializedOrder) {
        return deserializeUuidList(serializedOrder);
    }

    private String serializeUuidList(List<UUID> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private List<UUID> deserializeUuidList(String serializedValues) {
        if (serializedValues == null || serializedValues.isBlank()) {
            return List.of();
        }

        return Arrays.stream(serializedValues.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(UUID::fromString)
                .toList();
    }

    private String serializeVoteBallots(Map<UUID, UUID> ballots) {
        if (ballots == null || ballots.isEmpty()) {
            return null;
        }

        return ballots.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    private Map<UUID, UUID> deserializeVoteBallots(String serializedBallots) {
        if (serializedBallots == null || serializedBallots.isBlank()) {
            return new HashMap<>();
        }

        Map<UUID, UUID> ballots = new HashMap<>();
        for (String token : serializedBallots.split(",")) {
            String value = token.trim();
            if (value.isBlank()) {
                continue;
            }
            String[] pair = value.split(":");
            if (pair.length != 2) {
                continue;
            }
            ballots.put(UUID.fromString(pair[0].trim()), UUID.fromString(pair[1].trim()));
        }
        return ballots;
    }

    private Map<UUID, Integer> buildVoteTallyFromBallots(String serializedBallots, List<UUID> eligibleTargets) {
        Map<UUID, Integer> tallies = new HashMap<>();
        if (eligibleTargets != null) {
            for (UUID eligibleTarget : eligibleTargets) {
                tallies.put(eligibleTarget, 0);
            }
        }
        Map<UUID, UUID> ballots = deserializeVoteBallots(serializedBallots);
        for (UUID target : ballots.values()) {
            if (tallies.containsKey(target)) {
                tallies.put(target, tallies.get(target) + 1);
            }
        }
        return tallies;
    }

    private String serializeVoteTallies(Map<UUID, Integer> tallies) {
        if (tallies == null || tallies.isEmpty()) {
            return null;
        }
        return tallies.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    private Map<UUID, Integer> deserializeVoteTallies(String serializedTallies) {
        if (serializedTallies == null || serializedTallies.isBlank()) {
            return new HashMap<>();
        }

        Map<UUID, Integer> tallies = new HashMap<>();
        for (String token : serializedTallies.split(",")) {
            String value = token.trim();
            if (value.isBlank()) {
                continue;
            }
            String[] pair = value.split(":");
            if (pair.length != 2) {
                continue;
            }
            tallies.put(UUID.fromString(pair[0].trim()), Integer.parseInt(pair[1].trim()));
        }
        return tallies;
    }

    private String serializeScoreMap(Map<UUID, Integer> scoreMap) {
        return serializeVoteTallies(scoreMap);
    }

    private Map<UUID, Integer> deserializeScoreMap(String serializedScores) {
        return deserializeVoteTallies(serializedScores);
    }

    private Map<UUID, Integer> initializeScoreMap(List<ImposterGameLobbyMember> activeMembers) {
        Map<UUID, Integer> scoreMap = new HashMap<>();
        for (ImposterGameLobbyMember activeMember : activeMembers) {
            scoreMap.put(activeMember.getLearnerId(), 0);
        }
        return scoreMap;
    }

    private boolean isLobbyCodeUniqueViolation(Throwable error) {
        return hasConstraintViolation(error, LOBBY_CODE_UNIQUE_CONSTRAINT);
    }

    private boolean isMemberUniqueViolation(Throwable error) {
        return hasConstraintViolation(error, MEMBER_UNIQUE_CONSTRAINT);
    }

    private boolean hasConstraintViolation(Throwable error, String constraintName) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConstraintViolationException violationException) {
                return constraintName.equalsIgnoreCase(violationException.getConstraintName());
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
