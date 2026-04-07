package com.example.demo.me.imposter;

import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

class ImposterLobbyLifecycleSupport {

    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final Clock clock;
    private final String lobbyCodeConstraintName;
    private final String memberConstraintName;

    ImposterLobbyLifecycleSupport(
            ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository,
            Clock clock,
            String lobbyCodeConstraintName,
            String memberConstraintName
    ) {
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.clock = clock;
        this.lobbyCodeConstraintName = lobbyCodeConstraintName;
        this.memberConstraintName = memberConstraintName;
    }

    void applyDefaultSettings(
            ImposterGameLobby lobby,
            int defaultConceptCount,
            int defaultRoundsPerConcept,
            int defaultDiscussionTimerSeconds,
            int defaultImposterGuessTimerSeconds,
            int defaultTurnDurationSeconds,
            int defaultMaxVotingRounds
    ) {
        if (lobby.getConceptCount() == null) {
            lobby.setConceptCount(defaultConceptCount);
        }
        if (lobby.getRoundsPerConcept() == null) {
            lobby.setRoundsPerConcept(defaultRoundsPerConcept);
        }
        if (lobby.getDiscussionTimerSeconds() == null) {
            lobby.setDiscussionTimerSeconds(defaultDiscussionTimerSeconds);
        }
        if (lobby.getImposterGuessTimerSeconds() == null) {
            lobby.setImposterGuessTimerSeconds(defaultImposterGuessTimerSeconds);
        }
        if (lobby.getTurnDurationSeconds() == null) {
            lobby.setTurnDurationSeconds(defaultTurnDurationSeconds);
        }
        if (lobby.getMaxVotingRounds() == null) {
            lobby.setMaxVotingRounds(defaultMaxVotingRounds);
        }
        if (lobby.getStateVersion() == null) {
            lobby.setStateVersion(0);
        }
    }

    ImposterGameLobbyMember createMembership(ImposterGameLobby lobby, UUID learnerId) {
        ImposterGameLobbyMember member = new ImposterGameLobbyMember();
        member.setLobby(lobby);
        member.setLearnerId(learnerId);
        member.setJoinedAt(OffsetDateTime.now(clock));
        return imposterGameLobbyMemberRepository.saveAndFlush(member);
    }

    int defaultStateVersion(ImposterGameLobby lobby) {
        return lobby.getStateVersion() == null ? 0 : lobby.getStateVersion();
    }

    void incrementStateVersion(ImposterGameLobby lobby) {
        lobby.setStateVersion(defaultStateVersion(lobby) + 1);
    }

    boolean isLobbyCodeUniqueViolation(Throwable error) {
        return hasConstraintViolation(error, lobbyCodeConstraintName);
    }

    boolean isMemberUniqueViolation(Throwable error) {
        return hasConstraintViolation(error, memberConstraintName);
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
