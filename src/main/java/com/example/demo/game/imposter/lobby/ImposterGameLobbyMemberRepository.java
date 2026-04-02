package com.example.demo.game.imposter.lobby;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterGameLobbyMemberRepository extends JpaRepository<ImposterGameLobbyMember, Long> {

    Optional<ImposterGameLobbyMember> findByLobby_IdAndLearnerId(Long lobbyId, UUID learnerId);

    Optional<ImposterGameLobbyMember> findByLobby_IdAndLearnerIdAndLeftAtIsNull(Long lobbyId, UUID learnerId);

    boolean existsByLobby_IdAndLearnerId(Long lobbyId, UUID learnerId);
}
