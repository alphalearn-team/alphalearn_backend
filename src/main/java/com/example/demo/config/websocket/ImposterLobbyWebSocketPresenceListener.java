package com.example.demo.config.websocket;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.me.imposter.ImposterLobbyRealtimePresenceTracker;
import java.security.Principal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@ConditionalOnProperty(name = "imposter.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class ImposterLobbyWebSocketPresenceListener {

    private static final Pattern LOBBY_TOPIC_PATTERN = Pattern.compile("^/topic/imposter/lobbies/([0-9a-fA-F\\-]{36})$");
    private static final Pattern LOBBY_USER_QUEUE_PATTERN = Pattern.compile("^/user/queue/imposter/lobbies/([0-9a-fA-F\\-]{36})$");

    private final ImposterLobbyRealtimePresenceTracker presenceTracker;
    private final Clock clock;
    private final int disconnectGraceSeconds;

    public ImposterLobbyWebSocketPresenceListener(
            ImposterLobbyRealtimePresenceTracker presenceTracker,
            Clock clock,
            @Value("${imposter.lobby.realtime.disconnect-grace-seconds:30}") int disconnectGraceSeconds
    ) {
        this.presenceTracker = presenceTracker;
        this.clock = clock;
        this.disconnectGraceSeconds = disconnectGraceSeconds;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        UUID lobbyPublicId = extractLobbyPublicId(destination);
        if (lobbyPublicId == null) {
            return;
        }

        SupabaseAuthUser authUser = extractLearner(accessor.getUser());
        if (authUser == null || authUser.userId() == null) {
            return;
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        presenceTracker.registerLobbySubscription(
                sessionId,
                authUser.userId(),
                lobbyPublicId,
                OffsetDateTime.now(clock)
        );
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        presenceTracker.handleSessionDisconnect(sessionId, OffsetDateTime.now(clock), disconnectGraceSeconds);
    }

    private UUID extractLobbyPublicId(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        Matcher topicMatcher = LOBBY_TOPIC_PATTERN.matcher(destination);
        if (topicMatcher.matches()) {
            return UUID.fromString(topicMatcher.group(1));
        }
        Matcher userQueueMatcher = LOBBY_USER_QUEUE_PATTERN.matcher(destination);
        if (userQueueMatcher.matches()) {
            return UUID.fromString(userQueueMatcher.group(1));
        }
        return null;
    }

    private SupabaseAuthUser extractLearner(Principal principal) {
        if (principal instanceof AbstractAuthenticationToken authenticationToken
                && authenticationToken.getPrincipal() instanceof SupabaseAuthUser authUser
                && authUser.learner() != null) {
            return authUser;
        }
        return null;
    }
}
