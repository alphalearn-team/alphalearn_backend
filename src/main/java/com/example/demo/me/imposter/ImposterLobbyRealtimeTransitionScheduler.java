package com.example.demo.me.imposter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImposterLobbyRealtimeTransitionScheduler {

    private final LearnerImposterLobbyService learnerImposterLobbyService;
    private final LearnerImposterRankedMatchmakingService learnerImposterRankedMatchmakingService;

    public ImposterLobbyRealtimeTransitionScheduler(
            LearnerImposterLobbyService learnerImposterLobbyService,
            LearnerImposterRankedMatchmakingService learnerImposterRankedMatchmakingService
    ) {
        this.learnerImposterLobbyService = learnerImposterLobbyService;
        this.learnerImposterRankedMatchmakingService = learnerImposterRankedMatchmakingService;
    }

    @Scheduled(fixedDelayString = "${imposter.lobby.realtime.transition-poll-ms:1000}")
    public void processTimedTransitions() {
        learnerImposterLobbyService.processRealtimeTimedTransitions();
        learnerImposterLobbyService.processRealtimeDisconnectTimeouts();
        learnerImposterRankedMatchmakingService.processQueue();
    }
}
