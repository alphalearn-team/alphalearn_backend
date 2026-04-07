package com.example.demo.game.lobby;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ImposterLobbyRealtimePresenceTracker {

    private final ConcurrentHashMap<String, SessionState> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LobbyLearnerKey, PresenceState> presenceByLobbyLearner = new ConcurrentHashMap<>();

    public boolean registerLobbySubscription(String sessionId, UUID learnerId, UUID lobbyPublicId, OffsetDateTime now) {
        if (sessionId == null || learnerId == null || lobbyPublicId == null || now == null) {
            return false;
        }

        SessionState sessionState = sessionsById.computeIfAbsent(sessionId, ignored -> new SessionState(learnerId));
        boolean clearedReconnectingState = false;
        synchronized (sessionState) {
            if (!sessionState.learnerId().equals(learnerId)) {
                sessionState = new SessionState(learnerId);
                sessionsById.put(sessionId, sessionState);
            }

            if (sessionState.lobbyPublicIds().add(lobbyPublicId)) {
                LobbyLearnerKey key = new LobbyLearnerKey(lobbyPublicId, learnerId);
                PresenceState presence = presenceByLobbyLearner.computeIfAbsent(key, ignored -> new PresenceState());
                synchronized (presence) {
                    boolean wasReconnectPending = presence.disconnectDeadlineAt != null;
                    presence.activeSessionCount++;
                    presence.disconnectedAt = null;
                    presence.disconnectDeadlineAt = null;
                    clearedReconnectingState = clearedReconnectingState || wasReconnectPending;
                }
            }
        }
        return clearedReconnectingState;
    }

    public Set<UUID> handleSessionDisconnect(String sessionId, OffsetDateTime now, int graceSeconds) {
        Set<UUID> lobbiesNowReconnectPending = new HashSet<>();
        if (sessionId == null || now == null) {
            return lobbiesNowReconnectPending;
        }

        SessionState sessionState = sessionsById.remove(sessionId);
        if (sessionState == null) {
            return lobbiesNowReconnectPending;
        }

        Set<UUID> lobbyPublicIds;
        UUID learnerId;
        synchronized (sessionState) {
            learnerId = sessionState.learnerId();
            lobbyPublicIds = new HashSet<>(sessionState.lobbyPublicIds());
        }

        for (UUID lobbyPublicId : lobbyPublicIds) {
            LobbyLearnerKey key = new LobbyLearnerKey(lobbyPublicId, learnerId);
            PresenceState presence = presenceByLobbyLearner.get(key);
            if (presence == null) {
                continue;
            }

            synchronized (presence) {
                presence.activeSessionCount = Math.max(0, presence.activeSessionCount - 1);
                if (presence.activeSessionCount == 0) {
                    presence.disconnectedAt = now;
                    presence.disconnectDeadlineAt = now.plusSeconds(Math.max(1, graceSeconds));
                    lobbiesNowReconnectPending.add(lobbyPublicId);
                }
            }
        }
        return lobbiesNowReconnectPending;
    }

    public List<DisconnectTimeoutCandidate> consumeDueDisconnectTimeouts(OffsetDateTime now) {
        List<DisconnectTimeoutCandidate> due = new ArrayList<>();
        if (now == null) {
            return due;
        }

        for (var entry : presenceByLobbyLearner.entrySet()) {
            PresenceState state = entry.getValue();
            synchronized (state) {
                if (state.activeSessionCount == 0
                        && state.disconnectDeadlineAt != null
                        && !now.isBefore(state.disconnectDeadlineAt)) {
                    due.add(new DisconnectTimeoutCandidate(
                            entry.getKey().lobbyPublicId(),
                            entry.getKey().learnerId(),
                            Objects.requireNonNullElse(state.disconnectedAt, state.disconnectDeadlineAt)
                    ));
                    state.disconnectDeadlineAt = null;
                }
            }
        }
        return due;
    }

    public void clearLobbyMembership(UUID lobbyPublicId, UUID learnerId) {
        if (lobbyPublicId == null || learnerId == null) {
            return;
        }

        presenceByLobbyLearner.remove(new LobbyLearnerKey(lobbyPublicId, learnerId));
        sessionsById.forEach((sessionId, state) -> {
            if (!state.learnerId().equals(learnerId)) {
                return;
            }
            synchronized (state) {
                state.lobbyPublicIds().remove(lobbyPublicId);
            }
        });
    }

    public List<ReconnectPresenceSnapshot> listReconnectPresence(UUID lobbyPublicId) {
        List<ReconnectPresenceSnapshot> reconnecting = new ArrayList<>();
        if (lobbyPublicId == null) {
            return reconnecting;
        }

        for (var entry : presenceByLobbyLearner.entrySet()) {
            if (!entry.getKey().lobbyPublicId().equals(lobbyPublicId)) {
                continue;
            }
            PresenceState state = entry.getValue();
            synchronized (state) {
                if (state.activeSessionCount == 0 && state.disconnectDeadlineAt != null) {
                    reconnecting.add(new ReconnectPresenceSnapshot(entry.getKey().learnerId(), state.disconnectDeadlineAt));
                }
            }
        }
        reconnecting.sort((left, right) -> left.disconnectDeadlineAt().compareTo(right.disconnectDeadlineAt()));
        return reconnecting;
    }

    public record DisconnectTimeoutCandidate(UUID lobbyPublicId, UUID learnerId, OffsetDateTime disconnectedAt) {
    }

    public record ReconnectPresenceSnapshot(UUID learnerId, OffsetDateTime disconnectDeadlineAt) {
    }

    private record LobbyLearnerKey(UUID lobbyPublicId, UUID learnerId) {
    }

    private static final class SessionState {
        private final UUID learnerId;
        private final Set<UUID> lobbyPublicIds = new HashSet<>();

        private SessionState(UUID learnerId) {
            this.learnerId = learnerId;
        }

        public UUID learnerId() {
            return learnerId;
        }

        public Set<UUID> lobbyPublicIds() {
            return lobbyPublicIds;
        }
    }

    private static final class PresenceState {
        private int activeSessionCount;
        private OffsetDateTime disconnectedAt;
        private OffsetDateTime disconnectDeadlineAt;
    }
}
