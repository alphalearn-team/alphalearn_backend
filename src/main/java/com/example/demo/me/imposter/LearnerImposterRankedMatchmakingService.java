package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.matchmaking.ImposterRankedMatchmakingQueueEntry;
import com.example.demo.game.imposter.matchmaking.ImposterRankedMatchmakingQueueRepository;
import com.example.demo.game.imposter.matchmaking.ImposterRankedMatchmakingStatus;
import com.example.demo.game.imposter.realtime.ImposterLobbyRealtimePublisher;
import com.example.demo.me.imposter.dto.RankedImposterMatchmakingStatusDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerImposterRankedMatchmakingService {

    private final ImposterRankedMatchmakingQueueRepository queueRepository;
    private final LearnerImposterLobbyService learnerImposterLobbyService;
    private final ImposterLobbyRealtimePublisher realtimePublisher;
    private final Clock clock;

    public LearnerImposterRankedMatchmakingService(
            ImposterRankedMatchmakingQueueRepository queueRepository,
            LearnerImposterLobbyService learnerImposterLobbyService,
            ImposterLobbyRealtimePublisher realtimePublisher,
            Clock clock
    ) {
        this.queueRepository = queueRepository;
        this.learnerImposterLobbyService = learnerImposterLobbyService;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
    }

    @Transactional
    public RankedImposterMatchmakingStatusDto enqueue(SupabaseAuthUser user) {
        UUID learnerId = requireLearnerId(user);
        OffsetDateTime now = OffsetDateTime.now(clock);

        ImposterRankedMatchmakingQueueEntry queued = queueRepository.findQueuedByLearnerIdForUpdate(learnerId)
                .orElse(null);
        if (queued != null) {
            RankedImposterMatchmakingStatusDto dto = toDto(queued);
            publishStatus(learnerId, dto, "SEARCHING");
            return dto;
        }

        ImposterRankedMatchmakingQueueEntry entry = new ImposterRankedMatchmakingQueueEntry();
        entry.setLearnerId(learnerId);
        entry.setStatus(ImposterRankedMatchmakingStatus.QUEUED);
        entry.setQueuedAt(now);
        ImposterRankedMatchmakingQueueEntry saved = queueRepository.saveAndFlush(entry);

        RankedImposterMatchmakingStatusDto dto = toDto(saved);
        publishStatus(learnerId, dto, "SEARCHING");
        return dto;
    }

    @Transactional
    public RankedImposterMatchmakingStatusDto cancel(SupabaseAuthUser user) {
        UUID learnerId = requireLearnerId(user);
        ImposterRankedMatchmakingQueueEntry queued = queueRepository.findQueuedByLearnerIdForUpdate(learnerId)
                .orElse(null);
        if (queued == null) {
            RankedImposterMatchmakingStatusDto status = getStatus(user);
            publishStatus(learnerId, status, "CANCELLED");
            return status;
        }

        queued.setStatus(ImposterRankedMatchmakingStatus.CANCELLED);
        queued.setCancelledAt(OffsetDateTime.now(clock));
        ImposterRankedMatchmakingQueueEntry saved = queueRepository.saveAndFlush(queued);
        RankedImposterMatchmakingStatusDto dto = toDto(saved);
        publishStatus(learnerId, dto, "CANCELLED");
        return dto;
    }

    @Transactional(readOnly = true)
    public RankedImposterMatchmakingStatusDto getStatus(SupabaseAuthUser user) {
        UUID learnerId = requireLearnerId(user);
        return queueRepository.findTopByLearnerIdOrderByQueuedAtDesc(learnerId)
                .map(this::toDto)
                .orElse(new RankedImposterMatchmakingStatusDto("IDLE", null, null, null, null));
    }

    @Transactional
    public void processQueue() {
        List<ImposterRankedMatchmakingQueueEntry> queuedEntries = queueRepository.findQueuedEntriesForUpdate();
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (ImposterRankedMatchmakingQueueEntry entry : queuedEntries) {
            if (entry.getStatus() != ImposterRankedMatchmakingStatus.QUEUED) {
                continue;
            }
            UUID lobbyPublicId = learnerImposterLobbyService.assignLearnerToRankedLobbyAndMaybeStart(entry.getLearnerId());
            if (lobbyPublicId == null) {
                break;
            }

            entry.setStatus(ImposterRankedMatchmakingStatus.MATCHED);
            entry.setMatchedAt(now);
            entry.setAssignedLobbyPublicId(lobbyPublicId);
            ImposterRankedMatchmakingQueueEntry saved = queueRepository.saveAndFlush(entry);
            publishStatus(entry.getLearnerId(), toDto(saved), "MATCHED");
        }
    }

    private UUID requireLearnerId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user.userId();
    }

    private RankedImposterMatchmakingStatusDto toDto(ImposterRankedMatchmakingQueueEntry entry) {
        return new RankedImposterMatchmakingStatusDto(
                entry.getStatus().name(),
                entry.getAssignedLobbyPublicId(),
                entry.getQueuedAt(),
                entry.getMatchedAt(),
                entry.getCancelledAt()
        );
    }

    private void publishStatus(UUID learnerId, RankedImposterMatchmakingStatusDto status, String reason) {
        realtimePublisher.publishMatchmakingEvent(learnerId, status.lobbyPublicId(), reason, status);
    }
}
