package com.example.demo.config.websocket;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@ConditionalOnProperty(name = "imposter.websocket.enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketMessageBroker
public class GameWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final int WS_MESSAGE_SIZE_LIMIT_BYTES = 262_144;
    private static final int WS_SEND_BUFFER_SIZE_LIMIT_BYTES = 524_288;
    private static final int WS_SEND_TIME_LIMIT_MS = 20_000;
    private static final long STOMP_HEARTBEAT_INTERVAL_MS = 10_000L;

    public static final String IMP_LOBBY_TOPIC_PREFIX = "/topic/imposter/lobbies";
    public static final String IMP_APP_PREFIX = "/app";
    public static final String IMP_WS_ENDPOINT = "/ws";
    private final GameStompAuthChannelInterceptor imposterStompAuthChannelInterceptor;
    private final String[] allowedOriginPatterns;
    private final TaskScheduler stompHeartbeatTaskScheduler;

    public GameWebSocketConfig(
            GameStompAuthChannelInterceptor imposterStompAuthChannelInterceptor,
            @Qualifier("stompHeartbeatTaskScheduler") TaskScheduler stompHeartbeatTaskScheduler,
            @Value("${app.cors.allowed-origin-patterns:${app.cors.allowed-origins:${APP_CORS_ALLOWED_ORIGIN_PATTERNS:${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://127.0.0.1:3000}}}}") String allowedOriginPatterns
    ) {
        this.imposterStompAuthChannelInterceptor = imposterStompAuthChannelInterceptor;
        this.stompHeartbeatTaskScheduler = stompHeartbeatTaskScheduler;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(stompHeartbeatTaskScheduler)
                .setHeartbeatValue(new long[]{STOMP_HEARTBEAT_INTERVAL_MS, STOMP_HEARTBEAT_INTERVAL_MS});
        registry.setApplicationDestinationPrefixes(IMP_APP_PREFIX);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(IMP_WS_ENDPOINT)
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(imposterStompAuthChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(WS_MESSAGE_SIZE_LIMIT_BYTES);
        registry.setSendBufferSizeLimit(WS_SEND_BUFFER_SIZE_LIMIT_BYTES);
        registry.setSendTimeLimit(WS_SEND_TIME_LIMIT_MS);
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(WS_MESSAGE_SIZE_LIMIT_BYTES);
        container.setMaxBinaryMessageBufferSize(WS_MESSAGE_SIZE_LIMIT_BYTES);
        return container;
    }

}
