package com.example.demo.game.imposter.lobby;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImposterGameLobbyRepository extends JpaRepository<ImposterGameLobby, Long> {

    Optional<ImposterGameLobby> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lobby from ImposterGameLobby lobby where lobby.publicId = :publicId")
    Optional<ImposterGameLobby> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<ImposterGameLobby> findByLobbyCode(String lobbyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lobby from ImposterGameLobby lobby where lobby.lobbyCode = :lobbyCode")
    Optional<ImposterGameLobby> findByLobbyCodeForUpdate(@Param("lobbyCode") String lobbyCode);

    List<ImposterGameLobby> findByStartedAtIsNotNull();
}
