package com.example.demo.game.lobby.invite;

import com.example.demo.game.lobby.GameLobby;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "imposter_game_lobby_invites")
public class GameLobbyInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    @Setter(lombok.AccessLevel.NONE)
    private UUID publicId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lobby_id", nullable = false)
    private GameLobby lobby;

    @Column(name = "sender_learner_id", nullable = false, columnDefinition = "uuid")
    private UUID senderLearnerId;

    @Column(name = "receiver_learner_id", nullable = false, columnDefinition = "uuid")
    private UUID receiverLearnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GameLobbyInviteStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
