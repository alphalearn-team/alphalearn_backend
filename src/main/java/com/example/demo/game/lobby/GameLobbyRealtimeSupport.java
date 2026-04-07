package com.example.demo.game.lobby;

import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.realtime.GameLobbyRealtimePublisher;
import com.example.demo.learner.Learner;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class GameLobbyRealtimeSupport {

    private final GameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final GameLobbyStateAssembler stateAssembler;
    private final GameLobbyRealtimePublisher realtimePublisher;

    GameLobbyRealtimeSupport(
            GameLobbyMemberRepository imposterGameLobbyMemberRepository,
            GameLobbyStateAssembler stateAssembler,
            GameLobbyRealtimePublisher realtimePublisher
    ) {
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.stateAssembler = stateAssembler;
        this.realtimePublisher = realtimePublisher;
    }

    void publishRealtimeState(GameLobby lobby, String reason) {
        List<GameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        publishRealtimeState(lobby, reason, activeMembers);
    }

    void publishRealtimeState(
            GameLobby lobby,
            String reason,
            List<GameLobbyMember> activeMembers
    ) {
        Map<UUID, Learner> learnersById = stateAssembler.loadLearnersByIdForState(activeMembers, lobby);
        PrivateGameLobbyStateDto sharedBase = stateAssembler.buildLobbyState(lobby, null, activeMembers, learnersById);
        int stateVersion = lobby.getStateVersion() == null ? 0 : lobby.getStateVersion();
        realtimePublisher.publishSharedLobbyState(
                lobby.getPublicId(),
                stateVersion,
                reason,
                stateAssembler.buildSharedLobbyState(sharedBase)
        );

        for (GameLobbyMember activeMember : activeMembers) {
            PrivateGameLobbyStateDto viewerState = stateAssembler.buildLobbyState(
                    lobby,
                    activeMember.getLearnerId(),
                    activeMembers,
                    learnersById
            );
            realtimePublisher.publishViewerLobbyState(
                    lobby.getPublicId(),
                    activeMember.getLearnerId(),
                    stateVersion,
                    reason,
                    stateAssembler.buildViewerLobbyState(viewerState)
            );
        }
    }
}
