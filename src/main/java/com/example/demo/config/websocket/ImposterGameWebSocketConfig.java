package com.example.demo.config.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocketMessageBroker
public class ImposterGameWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final int WS_MESSAGE_SIZE_LIMIT_BYTES = 262_144;
    private static final int WS_SEND_BUFFER_SIZE_LIMIT_BYTES = 524_288;
    private static final int WS_SEND_TIME_LIMIT_MS = 20_000;

    public static final String IMP_LOBBY_TOPIC_PREFIX = "/topic/imposter/lobbies";
    public static final String IMP_APP_PREFIX = "/app";
    public static final String IMP_WS_ENDPOINT = "/ws";
    private final ImposterStompAuthChannelInterceptor imposterStompAuthChannelInterceptor;

    public ImposterGameWebSocketConfig(ImposterStompAuthChannelInterceptor imposterStompAuthChannelInterceptor) {
        this.imposterStompAuthChannelInterceptor = imposterStompAuthChannelInterceptor;
    }

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
