package com.example.demo.me.imposter;

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

    public void registerLobbySubscription(String sessionId, UUID learnerId, UUID lobbyPublicId, OffsetDateTime now) {
        if (sessionId == null || learnerId == null || lobbyPublicId == null || now == null) {
            return;
        }

        SessionState sessionState = sessionsById.computeIfAbsent(sessionId, ignored -> new SessionState(learnerId));
        synchronized (sessionState) {
            if (!sessionState.learnerId().equals(learnerId)) {
                sessionState = new SessionState(learnerId);
                sessionsById.put(sessionId, sessionState);
            }

            if (sessionState.lobbyPublicIds().add(lobbyPublicId)) {
                LobbyLearnerKey key = new LobbyLearnerKey(lobbyPublicId, learnerId);
                PresenceState presence = presenceByLobbyLearner.computeIfAbsent(key, ignored -> new PresenceState());
                synchronized (presence) {
                    presence.activeSessionCount++;
                    presence.disconnectedAt = null;
                    presence.disconnectDeadlineAt = null;
                }
            }
        }
    }

    public void handleSessionDisconnect(String sessionId, OffsetDateTime now, int graceSeconds) {
        if (sessionId == null || now == null) {
            return;
        }

        SessionState sessionState = sessionsById.remove(sessionId);
        if (sessionState == null) {
            return;
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
                }
            }
        }
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

    record DisconnectTimeoutCandidate(UUID lobbyPublicId, UUID learnerId, OffsetDateTime disconnectedAt) {
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
