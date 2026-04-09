package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.GameLobbyCodeGenerator;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import com.example.demo.game.lobby.GameLobbyPhase;
import com.example.demo.game.lobby.invite.GameLobbyInvite;
import com.example.demo.game.lobby.invite.GameLobbyInviteRepository;
import com.example.demo.game.lobby.invite.GameLobbyInviteStatus;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.learner.Learner;
import com.example.demo.game.lobby.dto.CreatePrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.LeavePrivateGameLobbyResponse;
import com.example.demo.game.lobby.dto.PrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyLeaveResult;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import com.example.demo.game.lobby.dto.SubmitGameGuessRequest;
import com.example.demo.game.lobby.dto.SubmitGameVoteRequest;
import com.example.demo.game.lobby.dto.UpdatePrivateGameLobbySettingsRequest;
import com.example.demo.game.lobby.dto.UpsertGameDrawingSnapshotRequest;
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

class GameLobbyOperationsSupport {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String MEMBER_REMOVED_REASON_KICKED_BY_HOST = "KICKED_BY_HOST";

    private final GameLobbyRepository lobbyRepository;
    private final GameLobbyMemberRepository memberRepository;
    private final GameLobbyCodeGenerator codeGenerator;
    private final GameMonthlyPackRepository monthlyPackRepository;
    private final GameMonthlyPackConceptRepository monthlyPackConceptRepository;
    private final GameLobbyInviteRepository inviteRepository;
    private final GameLobbyValidationSupport validationSupport;
    private final GameLobbyLifecycleSupport lifecycleSupport;
    private final GameLobbySerializationSupport serializationSupport;
    private final GameLobbyStateAssembler stateAssembler;
    private final GameLobbyRealtimeSupport realtimeSupport;
    private final GameLobbyGameFlowSupport gameFlowSupport;
    private final GameLobbyRoundEngine roundEngine;
    private final GameLobbyRealtimePresenceTracker presenceTracker;
    private final Clock clock;
    private final boolean liveDrawingEnabled;

    private final int lobbyCodeMaxRetries;
    private final int minActiveMembersToStart;
    private final int defaultConceptCount;
    private final int defaultRoundsPerConcept;
    private final int defaultDiscussionTimerSeconds;
    private final int defaultGameGuessTimerSeconds;
    private final int defaultTurnDurationSeconds;
    private final int defaultMaxVotingRounds;
    private final int minTimerSeconds;
    private final int maxTimerSeconds;
    private final String endReasonPlayerQuit;
    private final String endReasonPlayerDisconnectedTimeout;

    GameLobbyOperationsSupport(
            GameLobbyRepository lobbyRepository,
            GameLobbyMemberRepository memberRepository,
            GameLobbyCodeGenerator codeGenerator,
            GameMonthlyPackRepository monthlyPackRepository,
            GameMonthlyPackConceptRepository monthlyPackConceptRepository,
            GameLobbyInviteRepository inviteRepository,
            GameLobbyValidationSupport validationSupport,
            GameLobbyLifecycleSupport lifecycleSupport,
            GameLobbySerializationSupport serializationSupport,
            GameLobbyStateAssembler stateAssembler,
            GameLobbyRealtimeSupport realtimeSupport,
            GameLobbyGameFlowSupport gameFlowSupport,
            GameLobbyRoundEngine roundEngine,
            GameLobbyRealtimePresenceTracker presenceTracker,
            Clock clock,
            boolean liveDrawingEnabled,
            int lobbyCodeMaxRetries,
            int minActiveMembersToStart,
            int defaultConceptCount,
            int defaultRoundsPerConcept,
            int defaultDiscussionTimerSeconds,
            int defaultGameGuessTimerSeconds,
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
        this.inviteRepository = inviteRepository;
        this.validationSupport = validationSupport;
        this.lifecycleSupport = lifecycleSupport;
        this.serializationSupport = serializationSupport;
        this.stateAssembler = stateAssembler;
        this.realtimeSupport = realtimeSupport;
        this.gameFlowSupport = gameFlowSupport;
        this.roundEngine = roundEngine;
        this.presenceTracker = presenceTracker;
        this.clock = clock;
        this.liveDrawingEnabled = liveDrawingEnabled;
        this.lobbyCodeMaxRetries = lobbyCodeMaxRetries;
        this.minActiveMembersToStart = minActiveMembersToStart;
        this.defaultConceptCount = defaultConceptCount;
        this.defaultRoundsPerConcept = defaultRoundsPerConcept;
        this.defaultDiscussionTimerSeconds = defaultDiscussionTimerSeconds;
        this.defaultGameGuessTimerSeconds = defaultGameGuessTimerSeconds;
        this.defaultTurnDurationSeconds = defaultTurnDurationSeconds;
        this.defaultMaxVotingRounds = defaultMaxVotingRounds;
        this.minTimerSeconds = minTimerSeconds;
        this.maxTimerSeconds = maxTimerSeconds;
        this.endReasonPlayerQuit = endReasonPlayerQuit;
        this.endReasonPlayerDisconnectedTimeout = endReasonPlayerDisconnectedTimeout;
    }

    PrivateGameLobbyDto createPrivateLobby(SupabaseAuthUser user, CreatePrivateGameLobbyRequest request) {
        validationSupport.requireLearner(user);
        if (request == null || request.conceptPoolMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPoolMode is required");
        }
        String pinnedYearMonth = null;
        if (request.conceptPoolMode() == GameLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            String currentYearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
            GameMonthlyPack pack = monthlyPackRepository.findByYearMonth(currentYearMonth)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Current monthly imposter pack is not configured"));
            if (monthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId()).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Current monthly imposter pack has no concepts");
            }
            pinnedYearMonth = currentYearMonth;
        }

        for (int attempt = 1; attempt <= lobbyCodeMaxRetries; attempt++) {
            GameLobby lobby = new GameLobby();
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
                    defaultGameGuessTimerSeconds,
                    defaultTurnDurationSeconds,
                    defaultMaxVotingRounds
            );
            try {
                GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
                lifecycleSupport.createMembership(savedLobby, user.userId());
                return PrivateGameLobbyDto.from(savedLobby);
            } catch (DataIntegrityViolationException ex) {
                if (!lifecycleSupport.isLobbyCodeUniqueViolation(ex) || attempt == lobbyCodeMaxRetries) {
                    throw ex;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate lobby code");
    }

    JoinedPrivateGameLobbyDto joinPrivateLobby(SupabaseAuthUser user, JoinPrivateGameLobbyRequest request) {
        validationSupport.requireLearner(user);
        String lobbyCode = validationSupport.normalizeLobbyCode(request == null ? null : request.lobbyCode());
        if (lobbyCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode is required");
        }
        GameLobby lobby = lobbyRepository.findByLobbyCodeForUpdate(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game lobby not found"));
        GameLobbyMember activeMember = memberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId()).orElse(null);
        if (activeMember != null) {
            return JoinedPrivateGameLobbyDto.from(lobby, activeMember, true);
        }
        GameLobbyMember historicalMember = memberRepository.findByLobby_IdAndLearnerId(lobby.getId(), user.userId()).orElse(null);
        if (historicalMember != null) {
            historicalMember.setJoinedAt(OffsetDateTime.now(clock));
            historicalMember.setLeftAt(null);
            historicalMember.setRemovedByLearnerId(null);
            historicalMember.setRemovedReason(null);
            GameLobbyMember rejoinedMember = memberRepository.saveAndFlush(historicalMember);
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "JOIN_REJOIN");
            return JoinedPrivateGameLobbyDto.from(savedLobby, rejoinedMember, false);
        }
        try {
            GameLobbyMember createdMember = lifecycleSupport.createMembership(lobby, user.userId());
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "JOIN");
            return JoinedPrivateGameLobbyDto.from(savedLobby, createdMember, false);
        } catch (DataIntegrityViolationException ex) {
            if (lifecycleSupport.isMemberUniqueViolation(ex)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Learner already joined this lobby");
            }
            throw ex;
        }
    }

    LeavePrivateGameLobbyResponse leavePrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        validationSupport.requireLearner(user);
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        GameLobbyMember member = memberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Learner is not an active member of this lobby"));
        OffsetDateTime now = OffsetDateTime.now(clock);
        member.setLeftAt(now);
        memberRepository.saveAndFlush(member);
        presenceTracker.clearLobbyMembership(lobbyPublicId, user.userId());

        List<GameLobbyMember> remainingActiveMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        boolean leavingHost = user.userId().equals(lobby.getHostLearnerId());
        boolean startedAndActive = lobby.getStartedAt() != null
                && lobby.getCurrentPhase() != GameLobbyPhase.MATCH_COMPLETE
                && lobby.getCurrentPhase() != GameLobbyPhase.ABANDONED;
        if (startedAndActive) {
            roundEngine.abandonLobby(lobby, user.userId(), now, endReasonPlayerQuit);
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
            realtimeSupport.publishRealtimeState(savedLobby, "ABANDONED_BY_QUIT", remainingActiveMembers);
            return new LeavePrivateGameLobbyResponse(PrivateGameLobbyLeaveResult.LEFT_AND_SESSION_ABANDONED, state);
        }
        if (lobby.getCurrentPhase() == GameLobbyPhase.ABANDONED) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
            realtimeSupport.publishRealtimeState(savedLobby, "LEAVE_AFTER_ABANDONED", remainingActiveMembers);
            return new LeavePrivateGameLobbyResponse(PrivateGameLobbyLeaveResult.LEFT, state);
        }
        if (remainingActiveMembers.isEmpty()) {
            memberRepository.delete(member);
            try {
                lobbyRepository.deleteById(lobby.getId());
                lobbyRepository.flush();
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to close lobby because it is still referenced by active game data", ex);
            }
            return new LeavePrivateGameLobbyResponse(PrivateGameLobbyLeaveResult.LEFT_AND_LOBBY_DELETED, null);
        }
        if (leavingHost) {
            lobby.setHostLearnerId(remainingActiveMembers.get(0).getLearnerId());
        }
        gameFlowSupport.handleActiveGameOnMemberLeave(lobby, remainingActiveMembers, member.getLearnerId(), now);
        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyLeaveResult result = leavingHost
                ? PrivateGameLobbyLeaveResult.LEFT_AND_PROMOTED_HOST
                : PrivateGameLobbyLeaveResult.LEFT;
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
        realtimeSupport.publishRealtimeState(savedLobby, leavingHost ? "LEAVE_HOST_TRANSFER" : "LEAVE");
        return new LeavePrivateGameLobbyResponse(result, state);
    }

    PrivateGameLobbyStateDto updatePrivateLobbySettings(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpdatePrivateGameLobbySettingsRequest request
    ) {
        validationSupport.requireLearner(user);
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can update lobby settings");
        }
        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game lobby settings are locked after start");
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
            lobby.setGameGuessTimerSeconds(request.imposterGuessTimerSeconds());
        }
        if (request.turnDurationSeconds() != null) {
            validationSupport.validateRange(request.turnDurationSeconds(), minTimerSeconds, maxTimerSeconds, "turnDurationSeconds");
            lobby.setTurnDurationSeconds(request.turnDurationSeconds());
        }
        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId()));
        realtimeSupport.publishRealtimeState(savedLobby, "SETTINGS");
        return state;
    }

    PrivateGameLobbyStateDto startPrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        validationSupport.requireLearner(user);
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can start this imposter lobby");
        }
        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game lobby has already started");
        }
        boolean hostIsActiveMember = memberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId()).isPresent();
        if (!hostIsActiveMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby host must be an active member to start");
        }
        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.size() < minActiveMembersToStart) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least " + minActiveMembersToStart + " active players are required to start");
        }
        lifecycleSupport.applyDefaultSettings(
                lobby,
                defaultConceptCount,
                defaultRoundsPerConcept,
                defaultDiscussionTimerSeconds,
                defaultGameGuessTimerSeconds,
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
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "START");
        return state;
    }

    PrivateGameLobbyStateDto getPrivateLobbyState(SupabaseAuthUser user, UUID lobbyPublicId) {
        validationSupport.requireLearner(user);
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, OffsetDateTime.now(clock));
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        return stateAssembler.buildLobbyState(lobby, user.userId(), activeMembers);
    }

    PrivateGameLobbyStateDto kickPrivateLobbyMember(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UUID memberPublicId
    ) {
        validationSupport.requireLearner(user);
        if (memberPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberPublicId is required");
        }

        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());

        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can kick members");
        }
        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot kick members after lobby has started");
        }

        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        Map<UUID, Learner> learnersById = stateAssembler.loadLearnersByIdForState(activeMembers, lobby);

        UUID targetLearnerId = learnersById.values().stream()
                .filter(learner -> memberPublicId.equals(learner.getPublicId()))
                .map(Learner::getId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby member not found"));

        if (targetLearnerId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host cannot kick self");
        }

        GameLobbyMember targetMember = memberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), targetLearnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Target learner is not an active lobby member"));

        targetMember.setLeftAt(OffsetDateTime.now(clock));
        targetMember.setRemovedByLearnerId(user.userId());
        targetMember.setRemovedReason(MEMBER_REMOVED_REASON_KICKED_BY_HOST);
        memberRepository.saveAndFlush(targetMember);

        GameLobbyInvite pendingInvite = inviteRepository
                .findByLobby_IdAndReceiverLearnerIdAndStatus(lobby.getId(), targetLearnerId, GameLobbyInviteStatus.PENDING)
                .orElse(null);
        if (pendingInvite != null) {
            pendingInvite.setStatus(GameLobbyInviteStatus.CANCELED);
            pendingInvite.setRespondedAt(OffsetDateTime.now(clock));
            inviteRepository.save(pendingInvite);
        }

        presenceTracker.clearLobbyMembership(lobbyPublicId, targetLearnerId);
        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        List<GameLobbyMember> remainingActiveMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        realtimeSupport.publishRealtimeState(savedLobby, "KICK_MEMBER", remainingActiveMembers);
        return stateAssembler.buildLobbyState(savedLobby, user.userId(), remainingActiveMembers);
    }

    PrivateGameLobbyStateDto upsertDrawingSnapshot(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertGameDrawingSnapshotRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.snapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot is required");
        }
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);
        validationSupport.ensurePhase(lobby, GameLobbyPhase.DRAWING, "Drawing is not active");
        validationSupport.ensureViewerIsCurrentDrawer(lobby, user.userId());
        if (!liveDrawingEnabled) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Live drawing is disabled; submit drawing with done");
        }

        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "TURN_EXPIRED");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Drawing turn has ended");
        }

        Integer currentVersion = lobby.getDrawingVersion() == null ? 0 : lobby.getDrawingVersion();
        validationSupport.validateBaseVersion(request.baseVersion(), currentVersion);
        lobby.setCurrentDrawingSnapshot(request.snapshot());
        lobby.setDrawingVersion(currentVersion + 1);

        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "DRAWING_LIVE");
        return state;
    }

    PrivateGameLobbyStateDto submitDrawingDone(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            UpsertGameDrawingSnapshotRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.snapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot is required");
        }
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);
        validationSupport.ensurePhase(lobby, GameLobbyPhase.DRAWING, "Drawing is not active");
        validationSupport.ensureViewerIsCurrentDrawer(lobby, user.userId());

        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
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
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "DRAWING_DONE");
        return state;
    }

    PrivateGameLobbyStateDto submitVote(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitGameVoteRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.suspectedLearnerPublicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "suspectedLearnerPublicId is required");
        }
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);

        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned && lobby.getCurrentPhase() != GameLobbyPhase.VOTING) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        validationSupport.ensurePhase(lobby, GameLobbyPhase.VOTING, "Voting is not active");
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

        if (ballots.keySet().containsAll(activeMembers.stream().map(GameLobbyMember::getLearnerId).toList())) {
            gameFlowSupport.finalizeVotingRound(lobby, activeMembers, now);
        }
        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "VOTE");
        return state;
    }

    PrivateGameLobbyStateDto submitGameGuess(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            SubmitGameGuessRequest request
    ) {
        validationSupport.requireLearner(user);
        if (request == null || request.guess() == null || request.guess().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guess is required");
        }
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        validationSupport.ensureViewerIsMember(lobby, user.userId());
        validationSupport.ensureLobbyNotAbandoned(lobby);
        validationSupport.ensureLobbyStarted(lobby);

        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned && lobby.getCurrentPhase() != GameLobbyPhase.IMPOSTER_GUESS) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "STATE_RECONCILE");
            return stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        }
        validationSupport.ensurePhase(lobby, GameLobbyPhase.IMPOSTER_GUESS, "Game guess is not active");
        if (!user.userId().equals(lobby.getCurrentGameLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only current imposter can submit guess");
        }
        if (lobby.getGameGuessDeadlineAt() != null && !now.isBefore(lobby.getGameGuessDeadlineAt())) {
            gameFlowSupport.resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    GameLobbyGameFlowSupport.WinnerSide.NON_IMPOSTERS,
                    GameLobbyGameFlowSupport.ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game guess window has ended");
        }

        String guessed = request.guess().trim();
        boolean correct = guessed.equalsIgnoreCase(lobby.getCurrentConceptTitle() == null ? "" : lobby.getCurrentConceptTitle().trim());
        lobby.setLastGameGuess(guessed);
        lobby.setLastGameGuessCorrect(correct);
        gameFlowSupport.resolveConceptOutcome(
                lobby,
                activeMembers,
                now,
                correct ? GameLobbyGameFlowSupport.WinnerSide.IMPOSTER : GameLobbyGameFlowSupport.WinnerSide.NON_IMPOSTERS,
                correct ? GameLobbyGameFlowSupport.ConceptResolution.IMPOSTER_GUESS_CORRECT : GameLobbyGameFlowSupport.ConceptResolution.IMPOSTER_GUESS_WRONG,
                lobby.getVotedOutLearnerId(),
                guessed,
                false,
                serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies())
        );
        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        PrivateGameLobbyStateDto state = stateAssembler.buildLobbyState(savedLobby, user.userId(), activeMembers);
        realtimeSupport.publishRealtimeState(savedLobby, "GUESS");
        return state;
    }

    void processRealtimeTimedTransitions() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<UUID> startedLobbyPublicIds = lobbyRepository.findByStartedAtIsNotNull()
                .stream()
                .map(GameLobby::getPublicId)
                .toList();
        for (UUID lobbyPublicId : startedLobbyPublicIds) {
            processTimedTransitionsForLobby(lobbyPublicId, now);
        }
    }

    void processRealtimeDisconnectTimeouts() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<GameLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> dueDisconnects =
                presenceTracker.consumeDueDisconnectTimeouts(now);
        for (GameLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate dueDisconnect : dueDisconnects) {
            processDisconnectTimeout(dueDisconnect, now);
        }
    }

    void publishRealtimePresenceUpdate(UUID lobbyPublicId, String reason) {
        if (lobbyPublicId == null) {
            return;
        }
        GameLobby lobby = lobbyRepository.findByPublicId(lobbyPublicId).orElse(null);
        if (lobby == null) {
            return;
        }
        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        realtimeSupport.publishRealtimeState(lobby, reason, activeMembers);
    }

    private void processTimedTransitionsForLobby(UUID lobbyPublicId, OffsetDateTime now) {
        GameLobby lobby = validationSupport.resolveLobbyByPublicId(lobbyPublicId, true);
        if (lobby.getCurrentPhase() == GameLobbyPhase.ABANDONED) {
            return;
        }
        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        boolean transitioned = gameFlowSupport.resolveTimedTransitionsIfNeeded(lobby, activeMembers, now);
        if (transitioned) {
            lifecycleSupport.incrementStateVersion(lobby);
            GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
            realtimeSupport.publishRealtimeState(savedLobby, "DEADLINE_ADVANCE", activeMembers);
        }
    }

    private void processDisconnectTimeout(
            GameLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate dueDisconnect,
            OffsetDateTime now
    ) {
        GameLobby lobby;
        try {
            lobby = validationSupport.resolveLobbyByPublicId(dueDisconnect.lobbyPublicId(), true);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return;
            }
            throw ex;
        }
        if (lobby.getStartedAt() == null
                || lobby.getCurrentPhase() == GameLobbyPhase.MATCH_COMPLETE
                || lobby.getCurrentPhase() == GameLobbyPhase.ABANDONED) {
            return;
        }
        boolean learnerStillActive = memberRepository
                .existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), dueDisconnect.learnerId());
        if (!learnerStillActive) {
            return;
        }
        List<GameLobbyMember> activeMembers = memberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        if (activeMembers.isEmpty()) {
            return;
        }
        roundEngine.abandonLobby(lobby, dueDisconnect.learnerId(), now, endReasonPlayerDisconnectedTimeout);
        lifecycleSupport.incrementStateVersion(lobby);
        GameLobby savedLobby = lobbyRepository.saveAndFlush(lobby);
        realtimeSupport.publishRealtimeState(savedLobby, "ABANDONED_BY_DISCONNECT_TIMEOUT", activeMembers);
    }
}
