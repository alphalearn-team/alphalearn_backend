package com.example.demo.game.lobby;

import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyPhase;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.game.lobby.dto.GameConceptResolution;
import com.example.demo.game.lobby.dto.GameConceptWinnerSide;
import com.example.demo.game.lobby.dto.PrivateGameLobbyConceptResultDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyMemberStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyPlayerScoreDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyReconnectStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbySharedStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyViewerStateDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyVoteTallyDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

class GameLobbyStateAssembler {

    private final LearnerRepository learnerRepository;
    private final GameLobbyRealtimePresenceTracker realtimePresenceTracker;
    private final GameLobbySerializationSupport serializationSupport;
    private final int minActiveMembersToStart;
    private final int minTimerSeconds;
    private final int maxTimerSeconds;
    private final int defaultConceptCount;
    private final int defaultTurnDurationSeconds;
    private final int defaultMaxVotingRounds;

    GameLobbyStateAssembler(
            LearnerRepository learnerRepository,
            GameLobbyRealtimePresenceTracker realtimePresenceTracker,
            GameLobbySerializationSupport serializationSupport,
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

    PrivateGameLobbyStateDto buildLobbyState(
            GameLobby lobby,
            UUID viewerLearnerId,
            List<GameLobbyMember> activeMembers
    ) {
        Map<UUID, Learner> learnersById = loadLearnersByIdForState(activeMembers, lobby);
        return buildLobbyState(lobby, viewerLearnerId, activeMembers, learnersById);
    }

    PrivateGameLobbyStateDto buildLobbyState(
            GameLobby lobby,
            UUID viewerLearnerId,
            List<GameLobbyMember> activeMembers,
            Map<UUID, Learner> learnersById
    ) {
        List<PrivateGameLobbyMemberStateDto> activeMemberDtos = activeMembers.stream()
                .map(member -> {
                    Learner learner = learnersById.get(member.getLearnerId());
                    return new PrivateGameLobbyMemberStateDto(
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
        List<PrivateGameLobbyPlayerScoreDto> playerScores = playerScoreMap.entrySet().stream()
                .map(entry -> new PrivateGameLobbyPlayerScoreDto(
                        toLearnerPublicId(learnersById, entry.getKey()),
                        entry.getValue()
                ))
                .filter(entry -> entry.learnerPublicId() != null)
                .sorted(Comparator.comparingInt(PrivateGameLobbyPlayerScoreDto::points).reversed())
                .toList();

        List<UUID> eligibleVoteTargetPublicIds = serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()).stream()
                .map(targetId -> toLearnerPublicId(learnersById, targetId))
                .filter(id -> id != null)
                .toList();
        List<PrivateGameLobbyReconnectStateDto> reconnectingLearners = realtimePresenceTracker
                .listReconnectPresence(lobby.getPublicId())
                .stream()
                .map(snapshot -> new PrivateGameLobbyReconnectStateDto(
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
                && (lobby.getCurrentPhase() == null || lobby.getCurrentPhase() == GameLobbyPhase.DRAWING);
        boolean roundComplete = !drawingActive || lobby.getRoundCompletedAt() != null;
        boolean viewerIsGame = viewerLearnerId != null && viewerLearnerId.equals(lobby.getCurrentGameLearnerId());
        String viewerConceptTitle = viewerIsGame ? null : lobby.getCurrentConceptTitle();
        PrivateGameLobbyConceptResultDto latestConceptResult = buildLatestConceptResult(lobby, learnersById);

        return new PrivateGameLobbyStateDto(
                lobby.getPublicId(),
                lobby.getLobbyCode(),
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getConceptCount(),
                lobby.getRoundsPerConcept(),
                lobby.getDiscussionTimerSeconds(),
                lobby.getGameGuessTimerSeconds(),
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
                lobby.getGameGuessDeadlineAt(),
                viewerIsGame,
                viewerConceptTitle,
                lobby.getLastGameGuess(),
                lobby.getLastGameGuessCorrect()
        );
    }

    PrivateGameLobbySharedStateDto buildSharedLobbyState(PrivateGameLobbyStateDto state) {
        return new PrivateGameLobbySharedStateDto(
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
                state.lastGameGuess(),
                state.lastGameGuessCorrect()
        );
    }

    PrivateGameLobbyViewerStateDto buildViewerLobbyState(PrivateGameLobbyStateDto state) {
        return new PrivateGameLobbyViewerStateDto(
                state.viewerVoteTargetPublicId(),
                state.viewerIsGame(),
                state.viewerConceptTitle()
        );
    }

    Map<UUID, Learner> loadLearnersByIdForState(List<GameLobbyMember> activeMembers, GameLobby lobby) {
        Set<UUID> learnerIds = new LinkedHashSet<>(activeMembers.stream().map(GameLobbyMember::getLearnerId).toList());
        learnerIds.addAll(serializationSupport.deserializeScoreMap(lobby.getPlayerScores()).keySet());
        learnerIds.addAll(serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies()).keySet());
        learnerIds.addAll(serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
        learnerIds.add(lobby.getCurrentDrawerLearnerId());
        learnerIds.add(lobby.getVotedOutLearnerId());
        learnerIds.add(lobby.getCurrentGameLearnerId());
        learnerIds.add(lobby.getLatestResultAccusedLearnerId());
        learnerIds.add(lobby.getLatestResultGameLearnerId());
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

    private PrivateGameLobbyConceptResultDto buildLatestConceptResult(
            GameLobby lobby,
            Map<UUID, Learner> learnersById
    ) {
        if (lobby.getLatestResultConceptNumber() == null || lobby.getLatestResultWinnerSide() == null || lobby.getLatestResultResolution() == null) {
            return null;
        }

        GameConceptWinnerSide winnerSide = parseWinnerSide(lobby.getLatestResultWinnerSide());
        GameConceptResolution resolution = parseResolution(lobby.getLatestResultResolution());
        if (winnerSide == null || resolution == null) {
            return null;
        }

        List<PrivateGameLobbyVoteTallyDto> tallies = serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies()).entrySet()
                .stream()
                .map(entry -> new PrivateGameLobbyVoteTallyDto(
                        toLearnerPublicId(learnersById, entry.getKey()),
                        entry.getValue()
                ))
                .filter(entry -> entry.learnerPublicId() != null)
                .sorted(Comparator.comparingInt(PrivateGameLobbyVoteTallyDto::voteCount).reversed())
                .toList();

        return new PrivateGameLobbyConceptResultDto(
                lobby.getLatestResultConceptNumber(),
                lobby.getLatestResultConceptLabel(),
                winnerSide,
                resolution,
                toLearnerPublicId(learnersById, lobby.getLatestResultAccusedLearnerId()),
                toLearnerPublicId(learnersById, lobby.getLatestResultGameLearnerId()),
                Boolean.TRUE.equals(lobby.getLatestResultGameWinsByVotingTie()),
                lobby.getLatestResultGameGuess(),
                tallies
        );
    }

    private GameConceptWinnerSide parseWinnerSide(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GameConceptWinnerSide.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private GameConceptResolution parseResolution(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GameConceptResolution.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int defaultConceptCount(GameLobby lobby) {
        return lobby.getConceptCount() == null ? defaultConceptCount : lobby.getConceptCount();
    }

    private int defaultTurnDuration(GameLobby lobby) {
        return lobby.getTurnDurationSeconds() == null ? defaultTurnDurationSeconds : lobby.getTurnDurationSeconds();
    }

    private int defaultMaxVotingRounds(GameLobby lobby) {
        return lobby.getMaxVotingRounds() == null ? defaultMaxVotingRounds : lobby.getMaxVotingRounds();
    }

    private Integer defaultVersion(GameLobby lobby) {
        return lobby.getDrawingVersion() == null ? 0 : lobby.getDrawingVersion();
    }

    private Integer defaultStateVersion(GameLobby lobby) {
        return lobby.getStateVersion() == null ? 0 : lobby.getStateVersion();
    }
}
