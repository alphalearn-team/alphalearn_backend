package com.example.demo.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ImposterGameWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String IMP_LOBBY_TOPIC_PREFIX = "/topic/imposter/lobbies";
    public static final String IMP_APP_PREFIX = "/app";
    public static final String IMP_WS_ENDPOINT = "/ws";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes(IMP_APP_PREFIX);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(IMP_WS_ENDPOINT)
                .setAllowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000")
                .withSockJS();
    }
}
