package com.example.demo.game.imposter.lobby;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "imposter_game_lobbies")
public class ImposterGameLobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    @Setter(lombok.AccessLevel.NONE)
    private UUID publicId;

    @Column(name = "lobby_code", nullable = false, unique = true, length = 8)
    private String lobbyCode;

    @Column(name = "host_learner_id", columnDefinition = "uuid", nullable = false)
    private UUID hostLearnerId;

    @Column(name = "is_private", nullable = false)
    private boolean privateLobby = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "lobby_type", nullable = false, length = 32)
    private ImposterLobbyType lobbyType = ImposterLobbyType.PRIVATE_CUSTOM;

    @Enumerated(EnumType.STRING)
    @Column(name = "concept_pool_mode", nullable = false, length = 32)
    private ImposterLobbyConceptPoolMode conceptPoolMode;

    @Column(name = "pinned_year_month", length = 7)
    private String pinnedYearMonth;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "started_by_learner_id", columnDefinition = "uuid")
    private UUID startedByLearnerId;

    @Column(name = "concept_count")
    private Integer conceptCount;

    @Column(name = "rounds_per_concept")
    private Integer roundsPerConcept;

    @Column(name = "discussion_timer_seconds")
    private Integer discussionTimerSeconds;

    @Column(name = "imposter_guess_timer_seconds")
    private Integer imposterGuessTimerSeconds;

    @Column(name = "current_concept_index")
    private Integer currentConceptIndex;

    @Column(name = "current_concept_public_id", columnDefinition = "uuid")
    private UUID currentConceptPublicId;

    @Column(name = "current_concept_title", columnDefinition = "text")
    private String currentConceptTitle;

    @Column(name = "used_concept_public_ids", columnDefinition = "text")
    private String usedConceptPublicIds;

    @Column(name = "current_imposter_learner_id", columnDefinition = "uuid")
    private UUID currentImposterLearnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", length = 32)
    private ImposterLobbyPhase currentPhase;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "round_drawer_order", columnDefinition = "text")
    private String roundDrawerOrder;

    @Column(name = "current_turn_index")
    private Integer currentTurnIndex;

    @Column(name = "current_drawer_learner_id", columnDefinition = "uuid")
    private UUID currentDrawerLearnerId;

    @Column(name = "turn_started_at")
    private OffsetDateTime turnStartedAt;

    @Column(name = "turn_ends_at")
    private OffsetDateTime turnEndsAt;

    @Column(name = "turn_completed_at")
    private OffsetDateTime turnCompletedAt;

    @Column(name = "round_completed_at")
    private OffsetDateTime roundCompletedAt;

    @Column(name = "turn_duration_seconds")
    private Integer turnDurationSeconds;

    @Column(name = "current_drawing_snapshot", columnDefinition = "text")
    private String currentDrawingSnapshot;

    @Column(name = "drawing_version")
    private Integer drawingVersion;

    @Column(name = "voting_round_number")
    private Integer votingRoundNumber;

    @Column(name = "voting_eligible_target_learner_ids", columnDefinition = "text")
    private String votingEligibleTargetLearnerIds;

    @Column(name = "voting_ballots", columnDefinition = "text")
    private String votingBallots;

    @Column(name = "voting_deadline_at")
    private OffsetDateTime votingDeadlineAt;

    @Column(name = "voted_out_learner_id", columnDefinition = "uuid")
    private UUID votedOutLearnerId;

    @Column(name = "imposter_guess_deadline_at")
    private OffsetDateTime imposterGuessDeadlineAt;

    @Column(name = "last_imposter_guess", columnDefinition = "text")
    private String lastImposterGuess;

    @Column(name = "last_imposter_guess_correct")
    private Boolean lastImposterGuessCorrect;

    @Column(name = "player_scores", columnDefinition = "text")
    private String playerScores;

    @Column(name = "latest_result_concept_number")
    private Integer latestResultConceptNumber;

    @Column(name = "latest_result_concept_label", columnDefinition = "text")
    private String latestResultConceptLabel;

    @Column(name = "latest_result_winner_side", length = 32)
    private String latestResultWinnerSide;

    @Column(name = "latest_result_resolution", length = 64)
    private String latestResultResolution;

    @Column(name = "latest_result_accused_learner_id", columnDefinition = "uuid")
    private UUID latestResultAccusedLearnerId;

    @Column(name = "latest_result_imposter_learner_id", columnDefinition = "uuid")
    private UUID latestResultImposterLearnerId;

    @Column(name = "latest_result_imposter_wins_by_voting_tie")
    private Boolean latestResultImposterWinsByVotingTie;

    @Column(name = "latest_result_imposter_guess", columnDefinition = "text")
    private String latestResultImposterGuess;

    @Column(name = "latest_result_vote_tallies", columnDefinition = "text")
    private String latestResultVoteTallies;

    @Column(name = "concept_result_deadline_at")
    private OffsetDateTime conceptResultDeadlineAt;

    @Column(name = "max_voting_rounds")
    private Integer maxVotingRounds;

    @Column(name = "state_version")
    private Integer stateVersion;

    @Column(name = "ended_reason", length = 64)
    private String endedReason;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "abandoned_by_learner_id", columnDefinition = "uuid")
    private UUID abandonedByLearnerId;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
