package com.example.demo.game.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImposterLobbyRealtimePresenceTrackerTest {

    private final ImposterLobbyRealtimePresenceTracker tracker = new ImposterLobbyRealtimePresenceTracker();

    @Test
    void consumesTimeoutAfterGraceWhenSessionStaysDisconnected() {
        UUID learnerId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();
        String sessionId = "session-a";
        OffsetDateTime now = OffsetDateTime.parse("2026-04-02T00:00:00Z");

        tracker.registerLobbySubscription(sessionId, learnerId, lobbyPublicId, now);
        tracker.handleSessionDisconnect(sessionId, now, 30);

        List<ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> beforeGrace =
                tracker.consumeDueDisconnectTimeouts(now.plusSeconds(29));
        List<ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> afterGrace =
                tracker.consumeDueDisconnectTimeouts(now.plusSeconds(31));

        assertThat(beforeGrace).isEmpty();
        assertThat(afterGrace).hasSize(1);
        assertThat(afterGrace.get(0).lobbyPublicId()).isEqualTo(lobbyPublicId);
        assertThat(afterGrace.get(0).learnerId()).isEqualTo(learnerId);
    }

    @Test
    void reconnectBeforeGraceCancelsPendingTimeout() {
        UUID learnerId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();
        String sessionA = "session-a";
        String sessionB = "session-b";
        OffsetDateTime now = OffsetDateTime.parse("2026-04-02T00:00:00Z");

        tracker.registerLobbySubscription(sessionA, learnerId, lobbyPublicId, now);
        tracker.handleSessionDisconnect(sessionA, now, 30);
        boolean clearedReconnectState = tracker.registerLobbySubscription(sessionB, learnerId, lobbyPublicId, now.plusSeconds(10));

        List<ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> afterGrace =
                tracker.consumeDueDisconnectTimeouts(now.plusSeconds(31));

        assertThat(clearedReconnectState).isTrue();
        assertThat(afterGrace).isEmpty();
    }

    @Test
    void keepsPresenceActiveWhenOneOfMultipleSessionsDisconnects() {
        UUID learnerId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-04-02T00:00:00Z");

        tracker.registerLobbySubscription("session-a", learnerId, lobbyPublicId, now);
        tracker.registerLobbySubscription("session-b", learnerId, lobbyPublicId, now);
        tracker.handleSessionDisconnect("session-a", now.plusSeconds(2), 30);

        List<ImposterLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate> afterGrace =
                tracker.consumeDueDisconnectTimeouts(now.plusSeconds(40));

        assertThat(afterGrace).isEmpty();
    }

    @Test
    void listReconnectPresenceReturnsLearnerAndDeadlineWhenReconnectPending() {
        UUID learnerId = UUID.randomUUID();
        UUID lobbyPublicId = UUID.randomUUID();
        String sessionId = "session-a";
        OffsetDateTime now = OffsetDateTime.parse("2026-04-02T00:00:00Z");

        tracker.registerLobbySubscription(sessionId, learnerId, lobbyPublicId, now);
        Set<UUID> reconnectingLobbies = tracker.handleSessionDisconnect(sessionId, now.plusSeconds(1), 30);
        List<ImposterLobbyRealtimePresenceTracker.ReconnectPresenceSnapshot> reconnectPresence =
                tracker.listReconnectPresence(lobbyPublicId);

        assertThat(reconnectingLobbies).containsExactly(lobbyPublicId);
        assertThat(reconnectPresence).hasSize(1);
        assertThat(reconnectPresence.get(0).learnerId()).isEqualTo(learnerId);
        assertThat(reconnectPresence.get(0).disconnectDeadlineAt()).isEqualTo(now.plusSeconds(31));
    }
}
