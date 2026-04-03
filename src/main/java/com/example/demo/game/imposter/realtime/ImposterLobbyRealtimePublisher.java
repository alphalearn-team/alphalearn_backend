package com.example.demo.game.imposter.realtime;

import com.example.demo.config.websocket.ImposterGameWebSocketConfig;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ImposterLobbyRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public ImposterLobbyRealtimePublisher(SimpMessagingTemplate messagingTemplate, Clock clock) {
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    public String topicForLobby(UUID lobbyPublicId) {
        return ImposterGameWebSocketConfig.IMP_LOBBY_TOPIC_PREFIX + "/" + lobbyPublicId;
    }

    public void publishLobbyState(UUID lobbyPublicId, Integer stateVersion, String reason, Object state) {
        ImposterLobbyRealtimeEnvelope envelope = new ImposterLobbyRealtimeEnvelope(
                "LOBBY_STATE",
                lobbyPublicId,
                stateVersion,
                reason,
                OffsetDateTime.now(clock),
                state
        );
        messagingTemplate.convertAndSend(topicForLobby(lobbyPublicId), envelope);
    }
}
