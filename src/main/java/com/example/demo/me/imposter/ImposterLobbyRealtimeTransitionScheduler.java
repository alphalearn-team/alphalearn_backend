package com.example.demo.me.imposter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImposterLobbyRealtimeTransitionScheduler {

    private final LearnerImposterLobbyService learnerImposterLobbyService;

    public ImposterLobbyRealtimeTransitionScheduler(LearnerImposterLobbyService learnerImposterLobbyService) {
        this.learnerImposterLobbyService = learnerImposterLobbyService;
    }

    @Scheduled(fixedDelayString = "${imposter.lobby.realtime.transition-poll-ms:1000}")
    public void processTimedTransitions() {
        learnerImposterLobbyService.processRealtimeTimedTransitions();
    }
}
