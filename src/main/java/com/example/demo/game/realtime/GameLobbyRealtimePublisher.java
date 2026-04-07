package com.example.demo.game.realtime;

import com.example.demo.config.websocket.GameWebSocketConfig;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameLobbyRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public GameLobbyRealtimePublisher(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider, Clock clock) {
        this.messagingTemplate = messagingTemplateProvider.getIfAvailable();
        this.clock = clock;
    }

    public String topicForLobby(UUID lobbyPublicId) {
        return GameWebSocketConfig.IMP_LOBBY_TOPIC_PREFIX + "/" + lobbyPublicId;
    }

    public String userQueueForLobby(UUID lobbyPublicId) {
        return "/queue/imposter/lobbies/" + lobbyPublicId;
    }

    public void publishSharedLobbyState(UUID lobbyPublicId, Integer stateVersion, String reason, Object state) {
        if (messagingTemplate == null) {
            return;
        }

        GameLobbyRealtimeEnvelope envelope = new GameLobbyRealtimeEnvelope(
                "LOBBY_STATE",
                lobbyPublicId,
                stateVersion,
                reason,
                OffsetDateTime.now(clock),
                state
        );
        messagingTemplate.convertAndSend(topicForLobby(lobbyPublicId), envelope);
    }

    public void publishViewerLobbyState(
            UUID lobbyPublicId,
            UUID learnerId,
            Integer stateVersion,
            String reason,
            Object state
    ) {
        if (learnerId == null || messagingTemplate == null) {
            return;
        }

        GameLobbyRealtimeEnvelope envelope = new GameLobbyRealtimeEnvelope(
                "VIEWER_STATE",
                lobbyPublicId,
                stateVersion,
                reason,
                OffsetDateTime.now(clock),
                state
        );
        messagingTemplate.convertAndSendToUser(
                learnerId.toString(),
                userQueueForLobby(lobbyPublicId),
                envelope
        );
    }
}
