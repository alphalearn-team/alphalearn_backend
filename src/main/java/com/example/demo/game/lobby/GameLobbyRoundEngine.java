package com.example.demo.game.lobby;

import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyPhase;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class GameLobbyRoundEngine {

    private final GameLobbySerializationSupport serializationSupport;
    private final int conceptResultDurationSeconds;

    GameLobbyRoundEngine(
            GameLobbySerializationSupport serializationSupport,
            int conceptResultDurationSeconds
    ) {
        this.serializationSupport = serializationSupport;
        this.conceptResultDurationSeconds = conceptResultDurationSeconds;
    }

    void startVotingPhase(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now,
            int discussionTimerSeconds,
            Runnable onNoEligibleTargets
    ) {
        List<UUID> eligibleTargets = activeMembers.stream()
                .sorted(Comparator.comparing(GameLobbyMember::getJoinedAt))
                .map(GameLobbyMember::getLearnerId)
                .toList();
        if (eligibleTargets.isEmpty()) {
            onNoEligibleTargets.run();
            return;
        }

        lobby.setCurrentPhase(GameLobbyPhase.VOTING);
        lobby.setCurrentDrawerLearnerId(null);
        lobby.setCurrentTurnIndex(null);
        lobby.setTurnStartedAt(null);
        lobby.setTurnEndsAt(null);
        lobby.setTurnCompletedAt(now);
        lobby.setRoundCompletedAt(now);

        lobby.setVotingRoundNumber(1);
        lobby.setVotingEligibleTargetLearnerIds(serializationSupport.serializeUuidList(eligibleTargets));
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(now.plusSeconds(discussionTimerSeconds));
        lobby.setVotedOutLearnerId(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    void advanceToNextConceptOrCompleteMatch(
            GameLobby lobby,
            OffsetDateTime now,
            int conceptCount,
            Runnable initializeNextConcept
    ) {
        int currentConceptIndex = lobby.getCurrentConceptIndex() == null ? 1 : lobby.getCurrentConceptIndex();
        if (currentConceptIndex < conceptCount) {
            lobby.setCurrentConceptIndex(currentConceptIndex + 1);
            initializeNextConcept.run();
            return;
        }

        lobby.setCurrentPhase(GameLobbyPhase.MATCH_COMPLETE);
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
        lobby.setGameGuessDeadlineAt(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    void abandonLobby(
            GameLobby lobby,
            UUID abandonedByLearnerId,
            OffsetDateTime endedAt,
            String endedReason
    ) {
        lobby.setCurrentPhase(GameLobbyPhase.ABANDONED);
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
        lobby.setGameGuessDeadlineAt(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    int conceptResultDurationSeconds() {
        return conceptResultDurationSeconds;
    }
}
