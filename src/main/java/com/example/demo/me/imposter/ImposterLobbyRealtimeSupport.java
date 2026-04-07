package com.example.demo.me.imposter;

import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.realtime.ImposterLobbyRealtimePublisher;
import com.example.demo.learner.Learner;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyStateDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ImposterLobbyRealtimeSupport {

    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final ImposterLobbyStateAssembler stateAssembler;
    private final ImposterLobbyRealtimePublisher realtimePublisher;

    ImposterLobbyRealtimeSupport(
            ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository,
            ImposterLobbyStateAssembler stateAssembler,
            ImposterLobbyRealtimePublisher realtimePublisher
    ) {
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.stateAssembler = stateAssembler;
        this.realtimePublisher = realtimePublisher;
    }

    void publishRealtimeState(ImposterGameLobby lobby, String reason) {
        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());
        publishRealtimeState(lobby, reason, activeMembers);
    }

    void publishRealtimeState(
            ImposterGameLobby lobby,
            String reason,
            List<ImposterGameLobbyMember> activeMembers
    ) {
        Map<UUID, Learner> learnersById = stateAssembler.loadLearnersByIdForState(activeMembers, lobby);
        PrivateImposterLobbyStateDto sharedBase = stateAssembler.buildLobbyState(lobby, null, activeMembers, learnersById);
        int stateVersion = lobby.getStateVersion() == null ? 0 : lobby.getStateVersion();
        realtimePublisher.publishSharedLobbyState(
                lobby.getPublicId(),
                stateVersion,
                reason,
                stateAssembler.buildSharedLobbyState(sharedBase)
        );

        for (ImposterGameLobbyMember activeMember : activeMembers) {
            PrivateImposterLobbyStateDto viewerState = stateAssembler.buildLobbyState(
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
