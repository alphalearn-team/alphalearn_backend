package com.example.demo.game.lobby;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.GameWeeklyFeaturedConceptService;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import com.example.demo.game.lobby.GameLobbyPhase;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GameLobbyGameFlowSupport {

    enum WinnerSide {
        IMPOSTER,
        NON_IMPOSTERS
    }

    enum ConceptResolution {
        WRONG_ACCUSATION,
        VOTING_TIE_LIMIT,
        IMPOSTER_GUESS_CORRECT,
        IMPOSTER_GUESS_WRONG
    }

    private final GameLobbyLifecycleSupport lifecycleSupport;
    private final GameLobbySerializationSupport serializationSupport;
    private final GameLobbyRoundEngine roundEngine;
    private final GameMonthlyPackRepository imposterMonthlyPackRepository;
    private final GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;
    private final ConceptRepository conceptRepository;

    private final int defaultConceptCount;
    private final int defaultRoundsPerConcept;
    private final int defaultDiscussionTimerSeconds;
    private final int defaultGameGuessTimerSeconds;
    private final int defaultTurnDurationSeconds;
    private final int defaultMaxVotingRounds;

    GameLobbyGameFlowSupport(
            GameLobbyLifecycleSupport lifecycleSupport,
            GameLobbySerializationSupport serializationSupport,
            GameLobbyRoundEngine roundEngine,
            GameMonthlyPackRepository imposterMonthlyPackRepository,
            GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService,
            ConceptRepository conceptRepository,
            int defaultConceptCount,
            int defaultRoundsPerConcept,
            int defaultDiscussionTimerSeconds,
            int defaultGameGuessTimerSeconds,
            int defaultTurnDurationSeconds,
            int defaultMaxVotingRounds
    ) {
        this.lifecycleSupport = lifecycleSupport;
        this.serializationSupport = serializationSupport;
        this.roundEngine = roundEngine;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
        this.conceptRepository = conceptRepository;
        this.defaultConceptCount = defaultConceptCount;
        this.defaultRoundsPerConcept = defaultRoundsPerConcept;
        this.defaultDiscussionTimerSeconds = defaultDiscussionTimerSeconds;
        this.defaultGameGuessTimerSeconds = defaultGameGuessTimerSeconds;
        this.defaultTurnDurationSeconds = defaultTurnDurationSeconds;
        this.defaultMaxVotingRounds = defaultMaxVotingRounds;
    }

    void initializeConceptRuntime(GameLobby lobby, List<GameLobbyMember> activeMembers, OffsetDateTime now) {
        lifecycleSupport.applyDefaultSettings(
                lobby,
                defaultConceptCount,
                defaultRoundsPerConcept,
                defaultDiscussionTimerSeconds,
                defaultGameGuessTimerSeconds,
                defaultTurnDurationSeconds,
                defaultMaxVotingRounds
        );

        Concept selectedConcept = selectConceptForNextConceptSlot(lobby);
        lobby.setCurrentConceptPublicId(selectedConcept.getPublicId());
        lobby.setCurrentConceptTitle(selectedConcept.getTitle());

        Set<UUID> usedConceptPublicIds = new LinkedHashSet<>(serializationSupport.deserializeUuidList(lobby.getUsedConceptPublicIds()));
        usedConceptPublicIds.add(selectedConcept.getPublicId());
        lobby.setUsedConceptPublicIds(serializationSupport.serializeUuidList(new ArrayList<>(usedConceptPublicIds)));

        List<UUID> activeLearnerIds = activeMembers.stream().map(GameLobbyMember::getLearnerId).toList();
        UUID imposterLearnerId = activeLearnerIds.get(ThreadLocalRandom.current().nextInt(activeLearnerIds.size()));
        lobby.setCurrentGameLearnerId(imposterLearnerId);

        List<UUID> shuffledOrder = new ArrayList<>(activeLearnerIds);
        Collections.shuffle(shuffledOrder);

        lobby.setCurrentPhase(GameLobbyPhase.DRAWING);
        lobby.setRoundNumber(1);
        lobby.setRoundDrawerOrder(serializationSupport.serializeDrawerOrder(shuffledOrder));
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
        lobby.setGameGuessDeadlineAt(null);
        lobby.setLastGameGuess(null);
        lobby.setLastGameGuessCorrect(null);
        lobby.setConceptResultDeadlineAt(null);
    }

    Concept selectConceptForNextConceptSlot(GameLobby lobby) {
        Set<UUID> excluded = new LinkedHashSet<>(serializationSupport.deserializeUuidList(lobby.getUsedConceptPublicIds()));

        if (lobby.getConceptPoolMode() == GameLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            if (lobby.getPinnedYearMonth() == null || lobby.getPinnedYearMonth().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby monthly pack is not configured");
            }

            var featured = imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept(lobby.getPinnedYearMonth());
            if (featured.isPresent() && !excluded.contains(featured.get().getPublicId())) {
                return featured.get();
            }

            GameMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(lobby.getPinnedYearMonth())
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

    boolean resolveTimedTransitionsIfNeeded(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        boolean transitioned = false;
        if (lobby.getCurrentPhase() == GameLobbyPhase.VOTING
                && lobby.getVotingDeadlineAt() != null
                && !now.isBefore(lobby.getVotingDeadlineAt())) {
            finalizeVotingRound(lobby, activeMembers, now);
            transitioned = true;
        }
        if (lobby.getCurrentPhase() == GameLobbyPhase.DRAWING
                && lobby.getTurnEndsAt() != null
                && !now.isBefore(lobby.getTurnEndsAt())) {
            lobby.setTurnCompletedAt(now);
            advanceToNextDrawStepOrVoting(lobby, activeMembers, now);
            transitioned = true;
        }
        if (lobby.getCurrentPhase() == GameLobbyPhase.IMPOSTER_GUESS
                && lobby.getGameGuessDeadlineAt() != null
                && !now.isBefore(lobby.getGameGuessDeadlineAt())) {
            resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    WinnerSide.NON_IMPOSTERS,
                    ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
            transitioned = true;
        }
        if (lobby.getCurrentPhase() == GameLobbyPhase.CONCEPT_RESULT
                && lobby.getConceptResultDeadlineAt() != null
                && !now.isBefore(lobby.getConceptResultDeadlineAt())) {
            advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now);
            transitioned = true;
        }
        return transitioned;
    }

    void advanceToNextDrawStepOrVoting(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        List<UUID> drawerOrder = serializationSupport.deserializeDrawerOrder(lobby.getRoundDrawerOrder());
        if (drawerOrder.isEmpty()) {
            startVotingPhase(lobby, activeMembers, now);
            return;
        }

        Set<UUID> activeLearnerIds = activeMembers.stream().map(GameLobbyMember::getLearnerId).collect(Collectors.toSet());
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

        int roundsPerConcept = lobby.getRoundsPerConcept() == null ? defaultRoundsPerConcept : lobby.getRoundsPerConcept();
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

    void advanceCurrentTurnIfDrawerUnavailable(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        if (lobby.getStartedAt() == null || lobby.getCurrentDrawerLearnerId() == null) {
            return;
        }
        Set<UUID> activeLearnerIds = activeMembers.stream().map(GameLobbyMember::getLearnerId).collect(Collectors.toSet());
        if (activeLearnerIds.contains(lobby.getCurrentDrawerLearnerId())) {
            return;
        }
        advanceToNextDrawStepOrVoting(lobby, activeMembers, now);
    }

    void finalizeVotingRound(GameLobby lobby, List<GameLobbyMember> activeMembers, OffsetDateTime now) {
        Set<UUID> activeLearnerIds = activeMembers.stream().map(GameLobbyMember::getLearnerId).collect(Collectors.toSet());
        List<UUID> eligibleTargets = new ArrayList<>(serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
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
                    serializationSupport.buildVoteTallyFromBallots(lobby.getVotingBallots(), eligibleTargets)
            );
            return;
        }

        Map<UUID, UUID> ballots = serializationSupport.deserializeVoteBallots(lobby.getVotingBallots());
        ballots.entrySet().removeIf(entry -> !activeLearnerIds.contains(entry.getKey()) || !eligibleTargets.contains(entry.getValue()));
        lobby.setVotingBallots(serializationSupport.serializeVoteBallots(ballots));

        Map<UUID, Integer> tally = new HashMap<>();
        for (UUID target : ballots.values()) {
            tally.put(target, tally.getOrDefault(target, 0) + 1);
        }
        for (UUID eligibleTarget : eligibleTargets) {
            tally.putIfAbsent(eligibleTarget, 0);
        }
        lobby.setLatestResultVoteTallies(serializationSupport.serializeVoteTallies(tally));

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
            lobby.setCurrentPhase(GameLobbyPhase.VOTING);
            lobby.setVotingRoundNumber(currentRound + 1);
            lobby.setVotingEligibleTargetLearnerIds(serializationSupport.serializeUuidList(tiedTargets));
            lobby.setVotingBallots(null);
            lobby.setVotingDeadlineAt(now.plusSeconds(defaultDiscussionTimer(lobby)));
            lobby.setVotedOutLearnerId(null);
            return;
        }

        onVoteWinner(lobby, activeMembers, tiedTargets.get(0), now, tally);
    }

    void resolveConceptOutcome(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now,
            WinnerSide winnerSide,
            ConceptResolution resolution,
            UUID accusedLearnerId,
            String imposterGuess,
            boolean imposterWinsByVotingTie,
            Map<UUID, Integer> voteTallies
    ) {
        Map<UUID, Integer> scores = serializationSupport.deserializeScoreMap(lobby.getPlayerScores());
        for (GameLobbyMember activeMember : activeMembers) {
            scores.putIfAbsent(activeMember.getLearnerId(), 0);
        }

        if (winnerSide == WinnerSide.IMPOSTER) {
            scores.put(
                    lobby.getCurrentGameLearnerId(),
                    scores.getOrDefault(lobby.getCurrentGameLearnerId(), 0) + 1
            );
        } else {
            for (GameLobbyMember activeMember : activeMembers) {
                if (!activeMember.getLearnerId().equals(lobby.getCurrentGameLearnerId())) {
                    scores.put(activeMember.getLearnerId(), scores.getOrDefault(activeMember.getLearnerId(), 0) + 1);
                }
            }
        }

        lobby.setPlayerScores(serializationSupport.serializeScoreMap(scores));
        lobby.setLatestResultConceptNumber(lobby.getCurrentConceptIndex());
        lobby.setLatestResultConceptLabel(lobby.getCurrentConceptTitle());
        lobby.setLatestResultWinnerSide(winnerSide.name());
        lobby.setLatestResultResolution(resolution.name());
        lobby.setLatestResultAccusedLearnerId(accusedLearnerId);
        lobby.setLatestResultGameLearnerId(lobby.getCurrentGameLearnerId());
        lobby.setLatestResultGameWinsByVotingTie(imposterWinsByVotingTie);
        lobby.setLatestResultGameGuess(imposterGuess);
        lobby.setLatestResultVoteTallies(serializationSupport.serializeVoteTallies(voteTallies));
        lobby.setCurrentPhase(GameLobbyPhase.CONCEPT_RESULT);
        lobby.setConceptResultDeadlineAt(now.plusSeconds(roundEngine.conceptResultDurationSeconds()));
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
    }

    void handleActiveGameOnMemberLeave(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            UUID leftLearnerId,
            OffsetDateTime now
    ) {
        if (lobby.getStartedAt() == null || lobby.getCurrentPhase() == GameLobbyPhase.MATCH_COMPLETE) {
            return;
        }
        if (leftLearnerId.equals(lobby.getCurrentGameLearnerId())) {
            lobby.setCurrentGameLearnerId(activeMembers.get(0).getLearnerId());
        }
        if (lobby.getCurrentPhase() == null || lobby.getCurrentPhase() == GameLobbyPhase.DRAWING) {
            advanceCurrentTurnIfDrawerUnavailable(lobby, activeMembers, now);
            return;
        }
        if (lobby.getCurrentPhase() == GameLobbyPhase.VOTING) {
            List<UUID> eligible = new ArrayList<>(serializationSupport.deserializeUuidList(lobby.getVotingEligibleTargetLearnerIds()));
            Set<UUID> activeLearnerIds = activeMembers.stream().map(GameLobbyMember::getLearnerId).collect(Collectors.toSet());
            eligible.removeIf(id -> !activeLearnerIds.contains(id));
            lobby.setVotingEligibleTargetLearnerIds(serializationSupport.serializeUuidList(eligible));

            Map<UUID, UUID> ballots = serializationSupport.deserializeVoteBallots(lobby.getVotingBallots());
            ballots.entrySet().removeIf(entry -> !activeLearnerIds.contains(entry.getKey()) || !eligible.contains(entry.getValue()));
            lobby.setVotingBallots(serializationSupport.serializeVoteBallots(ballots));

            if (eligible.size() <= 1) {
                if (eligible.isEmpty()) {
                    advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now);
                } else {
                    onVoteWinner(
                            lobby,
                            activeMembers,
                            eligible.get(0),
                            now,
                            serializationSupport.buildVoteTallyFromBallots(lobby.getVotingBallots(), eligible)
                    );
                }
                return;
            }

            if (ballots.keySet().containsAll(activeLearnerIds)) {
                finalizeVotingRound(lobby, activeMembers, now);
            }
            return;
        }

        if (lobby.getCurrentPhase() == GameLobbyPhase.IMPOSTER_GUESS
                && !activeMembers.stream().map(GameLobbyMember::getLearnerId).toList().contains(lobby.getCurrentGameLearnerId())) {
            resolveConceptOutcome(
                    lobby,
                    activeMembers,
                    now,
                    WinnerSide.NON_IMPOSTERS,
                    ConceptResolution.IMPOSTER_GUESS_WRONG,
                    lobby.getVotedOutLearnerId(),
                    null,
                    false,
                    serializationSupport.deserializeVoteTallies(lobby.getLatestResultVoteTallies())
            );
        }
    }

    private void startVotingPhase(GameLobby lobby, List<GameLobbyMember> activeMembers, OffsetDateTime now) {
        roundEngine.startVotingPhase(
                lobby,
                activeMembers,
                now,
                defaultDiscussionTimer(lobby),
                () -> advanceToNextConceptOrCompleteMatch(lobby, activeMembers, now)
        );
    }

    private void onVoteWinner(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            UUID votedOutLearnerId,
            OffsetDateTime now,
            Map<UUID, Integer> voteTallies
    ) {
        lobby.setVotedOutLearnerId(votedOutLearnerId);

        if (!votedOutLearnerId.equals(lobby.getCurrentGameLearnerId())) {
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

        lobby.setCurrentPhase(GameLobbyPhase.IMPOSTER_GUESS);
        lobby.setGameGuessDeadlineAt(now.plusSeconds(defaultGameGuessTimer(lobby)));
        lobby.setTurnEndsAt(null);
    }

    private void advanceToNextConceptOrCompleteMatch(
            GameLobby lobby,
            List<GameLobbyMember> activeMembers,
            OffsetDateTime now
    ) {
        roundEngine.advanceToNextConceptOrCompleteMatch(
                lobby,
                now,
                defaultConceptCount(lobby),
                () -> initializeConceptRuntime(lobby, activeMembers, now)
        );
    }

    private int defaultConceptCount(GameLobby lobby) {
        return lobby.getConceptCount() == null ? defaultConceptCount : lobby.getConceptCount();
    }

    private int defaultDiscussionTimer(GameLobby lobby) {
        return lobby.getDiscussionTimerSeconds() == null ? defaultDiscussionTimerSeconds : lobby.getDiscussionTimerSeconds();
    }

    private int defaultGameGuessTimer(GameLobby lobby) {
        return lobby.getGameGuessTimerSeconds() == null ? defaultGameGuessTimerSeconds : lobby.getGameGuessTimerSeconds();
    }

    private int defaultTurnDuration(GameLobby lobby) {
        return lobby.getTurnDurationSeconds() == null ? defaultTurnDurationSeconds : lobby.getTurnDurationSeconds();
    }

    private int defaultMaxVotingRounds(GameLobby lobby) {
        return lobby.getMaxVotingRounds() == null ? defaultMaxVotingRounds : lobby.getMaxVotingRounds();
    }
}
