package com.example.demo.me.imposter;

import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterLobbyPhase;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.me.imposter.dto.ImposterConceptResolution;
import com.example.demo.me.imposter.dto.ImposterConceptWinnerSide;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyConceptResultDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyMemberStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyPlayerScoreDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyReconnectStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbySharedStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyViewerStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyVoteTallyDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

class ImposterLobbyStateAssembler {

    private final LearnerRepository learnerRepository;
    private final ImposterLobbyRealtimePresenceTracker realtimePresenceTracker;
    private final ImposterLobbySerializationSupport serializationSupport;
    private final int minActiveMembersToStart;
    private final int minTimerSeconds;
    private final int maxTimerSeconds;
    private final int defaultConceptCount;
    private final int defaultTurnDurationSeconds;
    private final int defaultMaxVotingRounds;

    ImposterLobbyStateAssembler(
            LearnerRepository learnerRepository,
            ImposterLobbyRealtimePresenceTracker realtimePresenceTracker,
            ImposterLobbySerializationSupport serializationSupport,
            int minActiveMembersToStart,
            int minTimerSeconds,
            int maxTimerSeconds,
            int defaultConceptCount,
            int defaultTurnDurationSeconds,
            int defaultMaxVotingRounds
    ) {
        this.learnerRepository = learnerRepository;
        this.realtimePresenceTracker = realtimePresenceTracker;
        this.serializationSupport = serializationSupport;
        this.minActiveMembersToStart = minActiveMembersToStart;
        this.minTimerSeconds = minTimerSeconds;
        this.maxTimerSeconds = maxTimerSeconds;
        this.defaultConceptCount = defaultConceptCount;
        this.defaultTurnDurationSeconds = defaultTurnDurationSeconds;
        this.defaultMaxVotingRounds = defaultMaxVotingRounds;
    }

    PrivateImposterLobbyStateDto buildLobbyState(
            ImposterGameLobby lobby,
            UUID viewerLearnerId,
            List<ImposterGameLobbyMember> activeMembers
    ) {
        Map<UUID, Learner> learnersById = loadLearnersByIdForState(activeMembers, lobby);
        return buildLobbyState(lobby, viewerLearnerId, activeMembers, learnersById);
    }

    PrivateImposterLobbyStateDto buildLobbyState(
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
        Map<UUID, UUID> ballots = serializationSupport.deserializeVoteBallots(lobby.getVotingBallots());
        UUID viewerVoteTargetLearnerId = viewerLearnerId == null ? null : ballots.get(viewerLearnerId);
        UUID viewerVoteTargetPublicId = toLearnerPublicId(learnersById, viewerVoteTargetLearnerId);
        Map<UUID, Integer> playerScoreMap = serializationSupport.deserializeScoreMap(lobby.getPlayerScores());
        List<PrivateImposterLobbyPlayerScoreDto> playerScores = playerScoreMap.entrySet().stream()
                .map(entry -> new PrivateImposterLobbyPlayerScoreDto(
                        toLearnerPublicId(learnersById, entry.getKey()),
                        entry.getValue()
                ))
                .filter(entry -> entry.learnerPublicId() != null)
                .sorted(Comparator.comparingInt(PrivateImposterLobbyPlayerScoreDto::points).reversed())
                .toList();

        List<UUID> eligibleVoteTargetPublicIds = serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()).stream()
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
        Integer totalTurns = serializationSupport.deserializeDrawerOrder(lobby.getRoundDrawerOrder()).size();
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
                viewerIsHost && viewerIsActiveMember && notStarted && activeMemberCount >= minActiveMembersToStart,
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
                minTimerSeconds,
                maxTimerSeconds,
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

    PrivateImposterLobbySharedStateDto buildSharedLobbyState(PrivateImposterLobbyStateDto state) {
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

    PrivateImposterLobbyViewerStateDto buildViewerLobbyState(PrivateImposterLobbyStateDto state) {
        return new PrivateImposterLobbyViewerStateDto(
                state.viewerVoteTargetPublicId(),
                state.viewerIsImposter(),
                state.viewerConceptTitle()
        );
    }

    Map<UUID, Learner> loadLearnersByIdForState(List<ImposterGameLobbyMember> activeMembers, ImposterGameLobby lobby) {
        Set<UUID> learnerIds = new LinkedHashSet<>(activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList());
        learnerIds.addAll(serializationSupport.deserializeScoreMap(lobby.getPlayerScores()).keySet());
        learnerIds.addAll(serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies()).keySet());
        learnerIds.addAll(serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
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

        List<PrivateImposterLobbyVoteTallyDto> tallies = serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies()).entrySet()
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

    private int defaultConceptCount(ImposterGameLobby lobby) {
        return lobby.getConceptCount() == null ? defaultConceptCount : lobby.getConceptCount();
    }

    private int defaultTurnDuration(ImposterGameLobby lobby) {
        return lobby.getTurnDurationSeconds() == null ? defaultTurnDurationSeconds : lobby.getTurnDurationSeconds();
    }

    private int defaultMaxVotingRounds(ImposterGameLobby lobby) {
        return lobby.getMaxVotingRounds() == null ? defaultMaxVotingRounds : lobby.getMaxVotingRounds();
    }

    private Integer defaultVersion(ImposterGameLobby lobby) {
        return lobby.getDrawingVersion() == null ? 0 : lobby.getDrawingVersion();
    }

    private Integer defaultStateVersion(ImposterGameLobby lobby) {
        return lobby.getStateVersion() == null ? 0 : lobby.getStateVersion();
    }
}
