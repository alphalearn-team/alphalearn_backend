package com.example.demo.game.lobby;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GameLobbyRealtimeTransitionScheduler {

    private final LearnerGameLobbyService learnerGameLobbyService;

    public GameLobbyRealtimeTransitionScheduler(LearnerGameLobbyService learnerGameLobbyService) {
        this.learnerGameLobbyService = learnerGameLobbyService;
    }

    @Scheduled(fixedDelayString = "${imposter.lobby.realtime.transition-poll-ms:1000}")
    public void processTimedTransitions() {
        learnerGameLobbyService.processRealtimeTimedTransitions();
        learnerGameLobbyService.processRealtimeDisconnectTimeouts();
    }
}
