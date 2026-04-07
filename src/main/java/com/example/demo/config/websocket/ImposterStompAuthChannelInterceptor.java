package com.example.demo.config.websocket;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseJwtAuthenticationConverter;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class ImposterStompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern LOBBY_TOPIC_PATTERN = Pattern.compile("^/topic/imposter/lobbies/([0-9a-fA-F\\-]{36})$");
    private static final Pattern LOBBY_USER_QUEUE_PATTERN = Pattern.compile("^/user/queue/imposter/lobbies/([0-9a-fA-F\\-]{36})$");
    private static final Pattern LOBBY_APP_PATTERN = Pattern.compile(
            "^/app/imposter/lobbies/([0-9a-fA-F\\-]{36})/(drawing/live|drawing/done|vote|guess)$"
    );

    private final JwtDecoder jwtDecoder;
    private final SupabaseJwtAuthenticationConverter authenticationConverter;
    private final ImposterGameLobbyRepository lobbyRepository;
    private final ImposterGameLobbyMemberRepository memberRepository;

    public ImposterStompAuthChannelInterceptor(
            JwtDecoder jwtDecoder,
            SupabaseJwtAuthenticationConverter authenticationConverter,
            ImposterGameLobbyRepository lobbyRepository,
            ImposterGameLobbyMemberRepository memberRepository
    ) {
        this.jwtDecoder = jwtDecoder;
        this.authenticationConverter = authenticationConverter;
        this.lobbyRepository = lobbyRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeLobbyDestination(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AccessDeniedException("Missing Authorization header for websocket connection");
        }

        String token = extractBearerToken(authorizationHeader);
        Jwt jwt = jwtDecoder.decode(token);
        AbstractAuthenticationToken authentication = authenticationConverter.convert(jwt);
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Unable to authenticate websocket connection");
        }

        accessor.setUser(authentication);
    }

    private void authorizeLobbyDestination(StompHeaderAccessor accessor) {
        Object principal = accessor.getUser();
        if (!(principal instanceof AbstractAuthenticationToken authenticationToken)
                || !(authenticationToken.getPrincipal() instanceof SupabaseAuthUser authUser)
                || authUser.userId() == null
                || authUser.learner() == null) {
            throw new AccessDeniedException("Learner authentication required for imposter websocket destinations");
        }

        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            return;
        }

        UUID lobbyPublicId = extractLobbyPublicId(destination);
        if (lobbyPublicId == null) {
            if (destination.startsWith("/app/imposter")
                    || destination.startsWith("/topic/imposter")
                    || destination.startsWith("/user/queue/imposter")) {
                throw new AccessDeniedException("Unsupported imposter websocket destination");
            }
            return;
        }

        ImposterGameLobby lobby = lobbyRepository.findByPublicId(lobbyPublicId)
                .orElseThrow(() -> new AccessDeniedException("Imposter lobby not found"));

        boolean isActiveMember = memberRepository.existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), authUser.userId());
        if (!isActiveMember) {
            throw new AccessDeniedException("Active lobby membership required");
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            boolean validTopicSubscribe = LOBBY_TOPIC_PATTERN.matcher(destination).matches();
            boolean validUserQueueSubscribe = LOBBY_USER_QUEUE_PATTERN.matcher(destination).matches();
            if (!validTopicSubscribe && !validUserQueueSubscribe) {
                throw new AccessDeniedException("Invalid imposter websocket subscribe destination");
            }
        }

        if (StompCommand.SEND.equals(accessor.getCommand()) && !LOBBY_APP_PATTERN.matcher(destination).matches()) {
            throw new AccessDeniedException("Invalid imposter websocket send destination");
        }
    }

    private UUID extractLobbyPublicId(String destination) {
        Matcher topicMatcher = LOBBY_TOPIC_PATTERN.matcher(destination);
        if (topicMatcher.matches()) {
            return UUID.fromString(topicMatcher.group(1));
        }

        Matcher userQueueMatcher = LOBBY_USER_QUEUE_PATTERN.matcher(destination);
        if (userQueueMatcher.matches()) {
            return UUID.fromString(userQueueMatcher.group(1));
        }

        Matcher appMatcher = LOBBY_APP_PATTERN.matcher(destination);
        if (appMatcher.matches()) {
            return UUID.fromString(appMatcher.group(1));
        }

        return null;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorizationHeader.substring(7).trim();
        }
        throw new AccessDeniedException("Authorization header must be a Bearer token");
    }
}
