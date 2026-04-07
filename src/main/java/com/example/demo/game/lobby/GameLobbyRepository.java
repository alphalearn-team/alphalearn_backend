package com.example.demo.game.lobby;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameLobbyRepository extends JpaRepository<GameLobby, Long> {

    Optional<GameLobby> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lobby from GameLobby lobby where lobby.publicId = :publicId")
    Optional<GameLobby> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<GameLobby> findByLobbyCode(String lobbyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lobby from GameLobby lobby where lobby.lobbyCode = :lobbyCode")
    Optional<GameLobby> findByLobbyCodeForUpdate(@Param("lobbyCode") String lobbyCode);

    List<GameLobby> findByStartedAtIsNotNull();
}
