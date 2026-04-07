package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyCodeGenerator;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.lobby.ImposterLobbyPhase;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.learner.Learner;
import com.example.demo.game.lobby.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.game.lobby.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.game.lobby.dto.LeavePrivateImposterLobbyResponse;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyDto;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyLeaveResult;
import com.example.demo.game.lobby.dto.PrivateImposterLobbyStateDto;
import com.example.demo.game.lobby.dto.SubmitImposterGuessRequest;
import com.example.demo.game.lobby.dto.SubmitImposterVoteRequest;
import com.example.demo.game.lobby.dto.UpdatePrivateImposterLobbySettingsRequest;
import com.example.demo.game.lobby.dto.UpsertImposterDrawingSnapshotRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ImposterLobbyOperationsSupport {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ImposterGameLobbyRepository lobbyRepository;
    private final ImposterGameLobbyMemberRepository memberRepository;
    private final ImposterLobbyCodeGenerator codeGenerator;
    private final ImposterMonthlyPackRepository monthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository monthlyPackConceptRepository;
    private final ImposterLobbyValidationSupport validationSupport;
    private final ImposterLobbyLifecycleSupport lifecycleSupport;
    private final ImposterLobbySerializationSupport serializationSupport;
    private final ImposterLobbyStateAssembler stateAssembler;
    private final ImposterLobbyRealtimeSupport realtimeSupport;
    private final ImposterLobbyGameFlowSupport gameFlowSupport;
    private final ImposterLobbyRoundEngine roundEngine;
    private final ImposterLobbyRealtimePresenceTracker presenceTracker;
    private final Clock clock;

    private final int lobbyCodeMaxRetries;
    private final int minActiveMembersToStart;
    private final int defaultConceptCount;
    private final int defaultRoundsPerConcept;
    private final int defaultDiscussionTimerSeconds;
    private final int defaultImposterGuessTimerSeconds;
    private final int defaultTurnDurationSeconds;
    private final int defaultMaxVotingRounds;
    private final int minTimerSeconds;
    private final int maxTimerSeconds;
    private final String endReasonPlayerQuit;
    private final String endReasonPlayerDisconnectedTimeout;

    ImposterLobbyOperationsSupport(
            ImposterGameLobbyRepository lobbyRepository,
            ImposterGameLobbyMemberRepository memberRepository,
            ImposterLobbyCodeGenerator codeGenerator,
            ImposterMonthlyPackRepository monthlyPackRepository,
            ImposterMonthlyPackConceptRepository monthlyPackConceptRepository,
            ImposterLobbyValidationSupport validationSupport,
            ImposterLobbyLifecycleSupport lifecycleSupport,
            ImposterLobbySerializationSupport serializationSupport,
            ImposterLobbyStateAssembler stateAssembler,
            ImposterLobbyRealtimeSupport realtimeSupport,
            ImposterLobbyGameFlowSupport gameFlowSupport,
            ImposterLobbyRoundEngine roundEngine,
            ImposterLobbyRealtimePresenceTracker presenceTracker,
            Clock clock,
            int lobbyCodeMaxRetries,
            int minActiveMembersToStart,
            int defaultConceptCount,
            int defaultRoundsPerConcept,
            int defaultDiscussionTimerSeconds,
            int defaultImposterGuessTimerSeconds,
            int defaultTurnDurationSeconds,
            int defaultMaxVotingRounds,
            int minTimerSeconds,
            int maxTimerSeconds,
            String endReasonPlayerQuit,
            String endReasonPlayerDisconnectedTimeout
    ) {
        this.lobbyRepository = lobbyRepository;
        this.memberRepository = memberRepository;
        this.codeGenerator = codeGenerator;
        this.monthlyPackRepository = monthlyPackRepository;
        this.monthlyPackConceptRepository = monthlyPackConceptRepository;
        this.validationSupport = validationSupport;
        this.lifecycleSupport = lifecycleSupport;
        this.serializationSupport = serializationSupport;
        this.stateAssembler = stateAssembler;
        this.realtimeSupport = realtimeSupport;
        this.gameFlowSupport = gameFlowSupport;
        this.roundEngine = roundEngine;
        this.presenceTracker = presenceTracker;
        this.clock = clock;
        this.lobbyCodeMaxRetries = lobbyCodeMaxRetries;
        this.minActiveMembersToStart = minActiveMembersToStart;
        this.defaultConceptCount = defaultConceptCount;
        this.defaultRoundsPerConcept = defaultRoundsPerConcept;
        this.defaultDiscussionTimerSeconds = defaultDiscussionTimerSeconds;
        this.defaultImposterGuessTimerSeconds = defaultImposterGuessTimerSeconds;
        this.defaultTurnDurationSeconds = defaultTurnDurationSeconds;
        this.defaultMaxVotingRounds = defaultMaxVotingRounds;
        this.minTimerSeconds = minTimerSeconds;
        this.maxTimerSeconds = maxTimerSeconds;
        this.endReasonPlayerQuit = endReasonPlayerQuit;
        this.endReasonPlayerDisconnectedTimeout = endReasonPlayerDisconnectedTimeout;
    }

    PrivateImposterLobbyDto createPrivateLobby(SupabaseAuthUser user, CreatePrivateImposterLobbyRequest request) {
        validationSupport.requireLearner(user);
        if (request == null || request.conceptPoolMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPoolMode is required");
        }
        String pinnedYearMonth = null;
        if (request.conceptPoolMode() == ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            String currentYearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
            ImposterMonthlyPack pack = monthlyPackRepository.findByYearMonth(currentYearMonth)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Current monthly imposter pack is not configured"));
            if (monthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId()).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Current monthly imposter pack has no concepts");
            }
            pinnedYearMonth = currentYearMonth;
        }

        for (int attempt = 1; attempt <= lobbyCodeMaxRetries; attempt++) {
            ImposterGameLobby lobby = new ImposterGameLobby();
            lobby.setLobbyCode(codeGenerator.generate());
            lobby.setHostLearnerId(user.userId());
            lobby.setPrivateLobby(true);
            lobby.setConceptPoolMode(request.conceptPoolMode());
            lobby.setPinnedYearMonth(pinnedYearMonth);
            lobby.setCreatedAt(OffsetDateTime.now(clock));
            lifecycleSupport.applyDefaultSettings(
                    lobby,
                    defaultConceptCount,
                    defaultRoundsPerConcept,
                    defaultDiscussionTimerSeconds,
                    defaultImposterGuessTimerSeconds,
                    defaultTurnDurationSeconds,
                    defaultMaxVotingRounds
            );
            try {
                ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
                lifecycleSupport.createMembership(savedLobby, user.userId());
                return PrivateImposterLobbyDto.from(savedLobby);
            } catch (DataIntegrityViolationException ex) {
                if (!lifecycleSupport.isLobbyCodeUniqueViolation(ex) || attempt == lobbyCodeMaxRetries) {
                    throw ex;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate lobby code");
    }

    JoinedPrivateImposterLobbyDto joinPrivateLobby(SupabaseAuthUser user, JoinPrivateImposterLobbyRequest request) {
        validationSupport.requireLearner(user);
        String lobbyCode = validationSupport.normalizeLobbyCode(request == null ? null : request.lobbyCode());
        if (lobbyCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode is required");
        }
        ImposterGameLobby lobby = lobbyRepository.findByLobbyCodeForUpdate(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));
        ImposterGameLobbyMember activeMember = memberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId()).orElse(null);
        if (activeMember != null) {
            return JoinedPrivateImposterLobbyDto.from(lobby, activeMember, true);
        }
        ImposterGameLobbyMember historicalMember = memberRepository.findByLobby_IdAndLearnerId(lobby.getId(), user.userId()).orElse(null);
        if (historicalMember != null) {
            historicalMember.setJoinedAt(OffsetDateTime.now(clock));
            historicalMember.setLeftAt(null);
            ImposterGameLobbyMember rejoinedMember = memberRepository.saveAndFlush(historicalMember);
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "JOIN_REJOIN");
            return JoinedPrivateImposterLobbyDto.from(savedLobby, rejoinedMember, false);
        }
        try {
            ImposterGameLobbyMember createdMember = lifecycleSupport.createMembership(lobby, user.userId());
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "JOIN");
            return JoinedPrivateImposterLobbyDto.from(savedLobby, createdMember, false);
        } catch (DataIntegrityViolationException ex) {
            if (lifecycleSupport.isMemberUniqueViolation(ex)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Learner already joined this lobby");
            }
            throw ex;
        }
    }

    LeavePrivateImposterLobbyResponse leavePrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        validationSupport.requireLearner(user);
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        ImposterGameLobbyMember member = memberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Learner is not an active member of this lobby"));
        OffsetDateTime now = OffsetDateTime.now(clock);
        member.setLeftAt(now);
        memberRepository.saveAndFlush(member);
        presenceTracker.clearLobbyMembership(lobbyPublicId, user.userId());

        List<ImposterGameLobbyMember> remainingActiveMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        boolean leavingHost = user.userId().equals(lobby.getHostLearnerId());
        boolean startedAndActive = lobby.getStartedAt() != null
                && lobby.getCurrentPhase() != ImposterLobbyPhase.MATCH_COMPLETE
                && lobby.getCurrentPhase() != ImposterLobbyPhase.ABANDONED;
        if (startedAndActive) {
            roundEngine.abandonLobby(lobby, user.userId(), now, endReasonPlayerQuit);
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
            realtimeSupport.publishRealtimeState(savedLobby, "ABANDONED_BY_QUIT", remainingActiveMembers);
            return new LeavePrivateImposterLobbyResponse(PrivateImposterLobbyLeaveResult.LEFT_AND_SESSION_ABANDONED, state);
        }
        if (lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
            realtimeSupport.publishRealtimeState(savedLobby, "LEAVE_AFTER_ABANDONED", remainingActiveMembers);
            return new LeavePrivateImposterLobbyResponse(PrivateImposterLobbyLeaveResult.LEFT, state);
        }
        if (remainingActiveMembers.isEmpty()) {
            memberRepository.delete(member);
            try {
                lobbyRepository.deleteById(lobby.getId());
                lobbyRepository.flush();
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to close lobby because it is still referenced by active game data", ex);
            }
            return new LeavePrivateImposterLobbyResponse(PrivateImposterLobbyLeaveResult.LEFT_AND_LOBBY_DELETED, null);
        }
        if (leavingHost) {
            lobby.setHostLearnerId(remainingActiveMembers.get(0).getLearnerId());
        }
        gameFlowSupport.handleActiveGameOnMemberLeave(lobby, remainingActiveMembers, member.getLearnerId(), now);
        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyLeaveResult result = leavingHost
                ? PrivateImposterLobbyLeaveResult.LEFT_AND_PROMOTED_HOST
                : PrivateImposterLobbyLeaveResult.LEFT;
        PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
        realtimeSupport.publishRealtimeState(savedLobby, leavingHost ? "LEAVE_HOST_TRANSFER" : "LEAVE");
        return new LeavePrivateImposterLobbyResponse(result, state);
    }

    PrivateImposterLobbyStateDto updatePrivateLobbySettings(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpdatePrivateImposterLobbySettingsRequest request
    ) {
        validationSupport.requireLearner(user);
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
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
            validationSupport.validateRange(request.conceptCount(), 1, 10, "conceptCount");
            lobby.setConceptCount(request.conceptCount());
        }
        if (request.roundsPerConcept() != null) {
            validationSupport.validateRange(request.roundsPerConcept(), 1, 3, "roundsPerConcept");
            lobby.setRoundsPerConcept(request.roundsPerConcept());
        }
        if (request.discussionTimerSeconds() != null) {
            validationSupport.validateRange(request.discussionTimerSeconds(), minTimerSeconds, maxTimerSeconds, "discussionTimerSeconds");
            lobby.setDiscussionTimerSeconds(request.discussionTimerSeconds());
        }
        if (request.imposterGuessTimerSeconds() != null) {
            validationSupport.validateRange(request.imposterGuessTimerSeconds(), minTimerSeconds, maxTimerSeconds, "imposterGuessTimerSeconds");
            lobby.setImposterGuessTimerSeconds(request.imposterGuessTimerSeconds());
        }
        if (request.turnDurationSeconds() != null) {
            validationSupport.validateRange(request.turnDurationSeconds(), minTimerSeconds, maxTimerSeconds, "turnDurationSeconds");
            lobby.setTurnDurationSeconds(request.turnDurationSeconds());
        }
        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId()));
        realtimeSupport.publishRealtimeState(savedLobby, "SETTINGS");
        return state;
    }

    PrivateImposterLobbyStateDto startPrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        validationSupport.requireLearner(user);
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can start this imposter lobby");
        }
        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby has already started");
        }
        boolean hostIsActiveMember = memberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId()).isPresent();
        if (!hostIsActiveMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby host must be an active member to start");
        }
        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.size() < minActiveMembersToStart) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least " + minActiveMembersToStart + " active players are required to start");
        }
        lifecycleSupport.applyDefaultSettings(
                lobby,
                defaultConceptCount,
                defaultRoundsPerConcept,
                defaultDiscussionTimerSeconds,
                defaultImposterGuessTimerSeconds,
                defaultTurnDurationSeconds,
                defaultMaxVotingRounds
        );
        OffsetDateTime now = OffsetDateTime.now(clock);
        lobby.setStartedAt(now);
        lobby.setStartedByLearnerId(user.userId());
        lobby.setCurrentConceptIndex(1);
        lobby.setUsedConceptPublicIds(null);
        lobby.setPlayerScores(serializationSupport.serializeScoreMap(serializationSupport.initializeScoreMap(activeMembers)));
        gameFlowSupport.initializeConceptRuntime(lobby, activeMembers, now);
        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "START");
        return state;
    }

    PrivateImposterLobbyStateDto getPrivateLobbyState(SupabaseAuthUser user, UUID lobbyPublicId) {
        validationSupport.requireLearner(user);
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, OffsetDateTime.now(clock));
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        return stateAssembler.buildLobbyState(lobby, user.userId(), activeMembers);
    }

    PrivateImposterLobbyStateDto upsertDrawingSnapshot(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.snapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot is required");
        }
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);
        validationSupport.ensurePhase(lobby, ImposterLobbyPhase.DRAWING, "Drawing is not active");
        validationSupport.ensureViewerIsCurrentDrawer(lobby, user.userId());
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Live drawing is disabled; submit drawing with done");
    }

    PrivateImposterLobbyStateDto submitDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertImposterDrawingSnapshotRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.snapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot is required");
        }
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);
        validationSupport.ensurePhase(lobby, ImposterLobbyPhase.DRAWING, "Drawing is not active");
        validationSupport.ensureViewerIsCurrentDrawer(lobby, user.userId());

        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "TURN_EXPIRED");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Drawing turn has ended");
        }
        Integer currentVersion = lobby.getDrawingVersion() == null ? 0 : lobby.getDrawingVersion();
        validationSupport.validateBaseVersion(request.baseVersion(), currentVersion);
        lobby.setCurrentDrawingSnapshot(request.snapshot());
        lobby.setDrawingVersion(currentVersion + 1);
        lobby.setTurnCompletedAt(now);
        gameFlowSupport.advanceToNextDrawStepOrVoting(lobby, activeMembers, now);

        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "DRAWING_DONE");
        return state;
    }

    PrivateImposterLobbyStateDto submitVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterVoteRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.suspectedLearnerPublicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "suspectedLearnerPublicId is required");
        }
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);

        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned && lobby.getCurrentPhase() != ImposterLobbyPhase.VOTING) {
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        validationSupport.ensurePhase(lobby, ImposterLobbyPhase.VOTING, "Voting is not active");
        if (lobby.getVotingDeadlineAt() != null && !now.isBefore(lobby.getVotingDeadlineAt())) {
            gameFlowSupport.finalizeVotingRound(lobby, activeMembers, now);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting window has ended");
        }

        Map<UUID, Learner> learnersById = stateAssembler.loadLearnersByIdForState(activeMembers, lobby);
        Map<UUID, UUID> learnerIdByPublicId = learnersById.values().stream().collect(Collectors.toMap(Learner::getPublicId, Learner::getId));
        UUID voteTargetLearnerId = learnerIdByPublicId.get(request.suspectedLearnerPublicId());
        if (voteTargetLearnerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vote target");
        }
        if (voteTargetLearnerId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot vote for yourself");
        }
        Set<UUID> eligibleTargets = new LinkedHashSet<>(serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
        if (!eligibleTargets.contains(voteTargetLearnerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vote target is not eligible in this voting round");
        }
        Map<UUID, UUID> ballots = serializationSupport.deserializeVoteBallots(lobby.getVotingBallots());
        ballots.put(user.userId(), voteTargetLearnerId);
        lobby.setVotingBallots(serializationSupport.serializeVoteBallots(ballots));

        if (ballots.keySet().containsAll(activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList())) {
            gameFlowSupport.finalizeVotingRound(lobby, activeMembers, now);
        }
        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "VOTE");
        return state;
    }

    PrivateImposterLobbyStateDto submitImposterGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitImposterGuessRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.guess() == null || request.guess().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guess is required");
        }
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);

        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned && lobby.getCurrentPhase() != ImposterLobbyPhase.IMPOSTER_GUESS) {
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        validationSupport.ensurePhase(lobby, ImposterLobbyPhase.IMPOSTER_GUESS, "Imposter guess is not active");
        if (!user.userId().equals(lobby.getCurrentImposterLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only current imposter can submit guess");
        }
        if (lobby.getImposterGuessDeadlineAt() != null && !now.isBefore(lobby.getImposterGuessDeadlineAt())) {
            gameFlowSupport.resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    ImposterLobbyGameFlowSupport.WinnerSide.NON_IMPOSTERS,
                    ImposterLobbyGameFlowSupport.ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter guess window has ended");
        }

        String guessed = request.guess().trim();
        boolean correct = guessed.equalsIgnoreCase(lobby.getCurrentConceptTitle() == null ? "" : lobby.getCurrentConceptTitle().trim());
        lobby.setLastImposterGuess(guessed);
        lobby.setLastImposterGuessCorrect(correct);
        gameFlowSupport.resolveConceptOutcome(
                lobby,
                activeMembers,
                now,
                correct ? ImposterLobbyGameFlowSupport.WinnerSide.IMPOSTER : ImposterLobbyGameFlowSupport.WinnerSide.NON_IMPOSTERS,
                correct ? ImposterLobbyGameFlowSupport.ConceptResolution.IMPOSTER_GUESS_CORRECT : ImposterLobbyGameFlowSupport.ConceptResolution.IMPOSTER_GUESS_WRONG,
                lobby.getVotedOutLearnerId(),
                guessed,
                false,
                serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies())
        );
        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateImposterLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "GUESS");
        return state;
    }

    void processRealtimeTimedTransitions() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<UUID> startedLobbyPublicIds = lobbyRepository.findByStartedAtIsNotNull()
                .stream()
                .map(ImposterGameLobby::getPublicId)
                .toList();
        for (UUID lobbyPublicId : startedLobbyPublicIds) {
            processTimedTransitionsForLobby(lobbyPublicId, now);
        }
    }

    void processRealtimeDisconnectTimeouts() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> dueDisconnects =
                presenceTracker.consumeDueDisconnectTimeouts(now);
        for (ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate dueDisconnect : dueDisconnects) {
            processDisconnectTimeout(dueDisconnect, now);
        }
    }

    void publishRealtimePresenceUpdate(UUID lobbyPublicId, String reason) {
        if (lobbyPublicId == null) {
            return;
        }
        ImposterGameLobby lobby = lobbyRepository.findByPublicId(lobbyPublicId).orElse(null);
        if (lobby == null) {
            return;
        }
        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        realtimeSupport.publishRealtimeState(lobby, reason, activeMembers);
    }

    private void processTimedTransitionsForLobby(UUID lobbyPublicId, OffsetDateTime now) {
        ImposterGameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        if (lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            return;
        }
        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "DEADLINE_ADVANCE", activeMembers);
        }
    }

    private void processDisconnectTimeout(
            ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate dueDisconnect,
            OffsetDateTime now
    ) {
        ImposterGameLobby lobby;
        try {
            lobby = validationSupport.resolveLobbyByPublicId(dueDisconnect.lobbyPublicId(), true);
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
        boolean learnerStillActive = memberRepository
                .existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), dueDisconnect.learnerId());
        if (!learnerStillActive) {
            return;
        }
        List<ImposterGameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        roundEngine.abandonLobby(lobby, dueDisconnect.learnerId(), now, endReasonPlayerDisconnectedTimeout);
        lifecycleSupport.incrementStateVersion(lobby);
        ImposterGameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        realtimeSupport.publishRealtimeState(savedLobby, "ABANDONED_BY_DISCONNECT_TIMEOUT", activeMembers);
    }
}
