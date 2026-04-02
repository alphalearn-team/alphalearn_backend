package com.example.demo.game.imposter.lobby;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterGameLobbyRepository extends JpaRepository<ImposterGameLobby, Long> {

    Optional<ImposterGameLobby> findByPublicId(UUID publicId);

    Optional<ImposterGameLobby> findByLobbyCode(String lobbyCode);
}
