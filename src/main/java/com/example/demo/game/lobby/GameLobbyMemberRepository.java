package com.example.demo.game.lobby;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameLobbyMemberRepository extends JpaRepository<GameLobbyMember, Long> {

    Optional<GameLobbyMember> findByLobby_IdAndLearnerId(Long lobbyId, UUID learnerId);

    Optional<GameLobbyMember> findByLobby_IdAndLearnerIdAndLeftAtIsNull(Long lobbyId, UUID learnerId);

    List<GameLobbyMember> findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(Long lobbyId);

    long countByLobby_IdAndLeftAtIsNull(Long lobbyId);

    boolean existsByLobby_IdAndLearnerId(Long lobbyId, UUID learnerId);

    boolean existsByLobby_IdAndLearnerIdAndLeftAtIsNull(Long lobbyId, UUID learnerId);
}
