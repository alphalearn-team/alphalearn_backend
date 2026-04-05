package com.example.demo.game.imposter.matchmaking;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImposterRankedMatchmakingQueueRepository extends JpaRepository<ImposterRankedMatchmakingQueueEntry, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from ImposterRankedMatchmakingQueueEntry entry where entry.learnerId = :learnerId and entry.status = 'QUEUED'")
    Optional<ImposterRankedMatchmakingQueueEntry> findQueuedByLearnerIdForUpdate(@Param("learnerId") UUID learnerId);

    Optional<ImposterRankedMatchmakingQueueEntry> findTopByLearnerIdOrderByQueuedAtDesc(UUID learnerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from ImposterRankedMatchmakingQueueEntry entry where entry.status = 'QUEUED' order by entry.queuedAt asc")
    List<ImposterRankedMatchmakingQueueEntry> findQueuedEntriesForUpdate();
}
