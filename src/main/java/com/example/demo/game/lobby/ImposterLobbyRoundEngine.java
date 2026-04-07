package com.example.demo.game.lobby;

import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterLobbyPhase;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class ImposterLobbyRoundEngine {

    private final ImposterLobbySerializationSupport serializationSupport;
    private final int conceptResultDurationSeconds;

    ImposterLobbyRoundEngine(
            ImposterLobbySerializationSupport serializationSupport,
            int conceptResultDurationSeconds
    ) {
        this.serializationSupport = serializationSupport;
        this.conceptResultDurationSeconds = conceptResultDurationSeconds;
    }

    void startVotingPhase(
            ImposterGameLobby lobby,
            List<ImposterGameLobbyMember> activeMembers,
            OffsetDateTime now,
            int discussionTimerSeconds,
            Runnable onNoEligibleTargets
    ) {
        List<UUID> eligibleTargets = activeMembers.stream()
                .sorted(Comparator.comparing(ImposterGameLobbyMember::getJoinedAt))
                .map(ImposterGameLobbyMember::getLearnerId)
                .toList();
        if (eligibleTargets.isEmpty()) {
            onNoEligibleTargets.run();
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
        lobby.setVotingEligibleTargetLearnerIds(serializationSupport.serializeUuidList(eligibleTargets));
        lobby.setVotingBallots(null);
        lobby.setVotingDeadlineAt(now.plusSeconds(discussionTimerSeconds));
        lobby.setVotedOutLearnerId(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    void advanceToNextConceptOrCompleteMatch(
            ImposterGameLobby lobby,
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

    void abandonLobby(
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

    int conceptResultDurationSeconds() {
        return conceptResultDurationSeconds;
    }
}
