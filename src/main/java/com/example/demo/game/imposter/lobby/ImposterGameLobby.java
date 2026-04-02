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
    @Column(name = "concept_pool_mode", nullable = false, length = 32)
    private ImposterLobbyConceptPoolMode conceptPoolMode;

    @Column(name = "pinned_year_month", length = 7)
    private String pinnedYearMonth;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
