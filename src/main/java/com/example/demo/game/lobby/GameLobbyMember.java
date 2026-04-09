package com.example.demo.game.lobby;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "imposter_game_lobby_members")
public class GameLobbyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lobby_id", nullable = false)
    private GameLobby lobby;

    @Column(name = "learner_id", nullable = false, columnDefinition = "uuid")
    private UUID learnerId;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "removed_by_learner_id", columnDefinition = "uuid")
    private UUID removedByLearnerId;

    @Column(name = "removed_reason", length = 64)
    private String removedReason;
}
