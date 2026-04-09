package com.example.demo.game.lobby.invite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameLobbyInviteRepository extends JpaRepository<GameLobbyInvite, Long> {

    Optional<GameLobbyInvite> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invite from GameLobbyInvite invite where invite.publicId = :publicId")
    Optional<GameLobbyInvite> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<GameLobbyInvite> findByLobby_IdAndReceiverLearnerIdAndStatus(
            Long lobbyId,
            UUID receiverLearnerId,
            GameLobbyInviteStatus status
    );

    List<GameLobbyInvite> findByReceiverLearnerIdAndStatusOrderByCreatedAtDesc(
            UUID receiverLearnerId,
            GameLobbyInviteStatus status
    );

    List<GameLobbyInvite> findBySenderLearnerIdAndStatusOrderByCreatedAtDesc(
            UUID senderLearnerId,
            GameLobbyInviteStatus status
    );
}
