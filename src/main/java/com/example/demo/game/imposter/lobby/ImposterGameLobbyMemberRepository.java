package com.example.demo.game.imposter.lobby;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterGameLobbyMemberRepository extends JpaRepository<ImposterGameLobbyMember, Long> {

    Optional<ImposterGameLobbyMember> findByLobby_IdAndLearnerId(Long lobbyId, UUID learnerId);

    Optional<ImposterGameLobbyMember> findByLobby_IdAndLearnerIdAndLeftAtIsNull(Long lobbyId, UUID learnerId);

    List<ImposterGameLobbyMember> findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(Long lobbyId);

    long countByLobby_IdAndLeftAtIsNull(Long lobbyId);

    boolean existsByLobby_IdAndLearnerId(Long lobbyId, UUID learnerId);

    boolean existsByLobby_IdAndLearnerIdAndLeftAtIsNull(Long lobbyId, UUID learnerId);
}
