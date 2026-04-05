package com.example.demo.me.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.matchmaking.ImposterRankedMatchmakingQueueEntry;
import com.example.demo.game.imposter.matchmaking.ImposterRankedMatchmakingQueueRepository;
import com.example.demo.game.imposter.matchmaking.ImposterRankedMatchmakingStatus;
import com.example.demo.game.imposter.realtime.ImposterLobbyRealtimePublisher;
import com.example.demo.learner.Learner;
import com.example.demo.me.imposter.dto.RankedImposterMatchmakingStatusDto;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearnerImposterRankedMatchmakingServiceTest {

    @Mock
    private ImposterRankedMatchmakingQueueRepository queueRepository;

    @Mock
    private LearnerImposterLobbyService learnerImposterLobbyService;

    @Mock
    private ImposterLobbyRealtimePublisher imposterLobbyRealtimePublisher;

    private LearnerImposterRankedMatchmakingService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-05T00:00:00Z"), ZoneOffset.UTC);
        service = new LearnerImposterRankedMatchmakingService(
                queueRepository,
                learnerImposterLobbyService,
                imposterLobbyRealtimePublisher,
                fixedClock
        );
        lenient().when(queueRepository.saveAndFlush(any(ImposterRankedMatchmakingQueueEntry.class)))
                .thenAnswer(invocation -> {
                    ImposterRankedMatchmakingQueueEntry entry = invocation.getArgument(0);
                    if (entry.getId() == null) {
                        ReflectionTestUtils.setField(entry, "id", 1L);
                    }
                    return entry;
                });
    }

    @Test
    void enqueueCreatesQueueEntryWhenMissing() {
        SupabaseAuthUser user = learnerAuthUser();
        when(queueRepository.findQueuedByLearnerIdForUpdate(user.userId())).thenReturn(Optional.empty());

        RankedImposterMatchmakingStatusDto result = service.enqueue(user);

        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(result.queuedAt()).isEqualTo(OffsetDateTime.parse("2026-04-05T00:00:00Z"));
        verify(imposterLobbyRealtimePublisher).publishMatchmakingEvent(eq(user.userId()), eq(null), eq("SEARCHING"), any());
    }

    @Test
    void cancelMarksQueuedEntryCancelled() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterRankedMatchmakingQueueEntry queued = new ImposterRankedMatchmakingQueueEntry();
        queued.setLearnerId(user.userId());
        queued.setStatus(ImposterRankedMatchmakingStatus.QUEUED);
        queued.setQueuedAt(OffsetDateTime.parse("2026-04-05T00:00:00Z"));
        when(queueRepository.findQueuedByLearnerIdForUpdate(user.userId())).thenReturn(Optional.of(queued));

        RankedImposterMatchmakingStatusDto result = service.cancel(user);

        assertThat(result.status()).isEqualTo("CANCELLED");
        assertThat(result.cancelledAt()).isEqualTo(OffsetDateTime.parse("2026-04-05T00:00:00Z"));
    }

    @Test
    void processQueueMatchesLearnerWhenLobbyAssigned() {
        UUID learnerId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();
        ImposterRankedMatchmakingQueueEntry queued = new ImposterRankedMatchmakingQueueEntry();
        queued.setLearnerId(learnerId);
        queued.setStatus(ImposterRankedMatchmakingStatus.QUEUED);
        queued.setQueuedAt(OffsetDateTime.parse("2026-04-05T00:00:00Z"));

        when(queueRepository.findQueuedEntriesForUpdate()).thenReturn(List.of(queued));
        when(learnerImposterLobbyService.assignLearnerToRankedLobbyAndMaybeStart(learnerId)).thenReturn(lobbyPublicId);

        service.processQueue();

        assertThat(queued.getStatus()).isEqualTo(ImposterRankedMatchmakingStatus.MATCHED);
        assertThat(queued.getAssignedLobbyPublicId()).isEqualTo(lobbyPublicId);
        verify(imposterLobbyRealtimePublisher).publishMatchmakingEvent(eq(learnerId), eq(lobbyPublicId), eq("MATCHED"), any());
    }

    @Test
    void processQueueLeavesLearnerQueuedWhenNoCapacity() {
        UUID learnerId = UUID.randomUUID();
        ImposterRankedMatchmakingQueueEntry queued = new ImposterRankedMatchmakingQueueEntry();
        queued.setLearnerId(learnerId);
        queued.setStatus(ImposterRankedMatchmakingStatus.QUEUED);
        queued.setQueuedAt(OffsetDateTime.parse("2026-04-05T00:00:00Z"));

        when(queueRepository.findQueuedEntriesForUpdate()).thenReturn(List.of(queued));
        when(learnerImposterLobbyService.assignLearnerToRankedLobbyAndMaybeStart(learnerId)).thenReturn(null);

        service.processQueue();

        assertThat(queued.getStatus()).isEqualTo(ImposterRankedMatchmakingStatus.QUEUED);
        verify(queueRepository, never()).saveAndFlush(any(ImposterRankedMatchmakingQueueEntry.class));
    }

    private SupabaseAuthUser learnerAuthUser() {
        UUID userId = UUID.randomUUID();
        Learner learner = new Learner();
        learner.setId(userId);
        learner.setPublicId(UUID.randomUUID());
        learner.setUsername("learner-" + userId.toString().substring(0, 8));
        learner.setCreatedAt(OffsetDateTime.parse("2026-04-05T00:00:00Z"));
        learner.setTotalPoints((short) 0);
        return new SupabaseAuthUser(userId, learner, null);
    }
}
