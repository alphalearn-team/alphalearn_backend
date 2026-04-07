package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyPhase;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ImposterLobbyValidationSupport {

    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final String drawingVersionConflictMessage;

    ImposterLobbyValidationSupport(
            ImposterGameLobbyRepository imposterGameLobbyRepository,
            ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository,
            String drawingVersionConflictMessage
    ) {
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.drawingVersionConflictMessage = drawingVersionConflictMessage;
    }

    SupabaseAuthUser requireLearner(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user;
    }

    ImposterGameLobby resolveLobbyByPublicId(UUID lobbyPublicId, boolean lockForUpdate) {
        if (lobbyPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyPublicId is required");
        }

        return (lockForUpdate
                ? imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)
                : imposterGameLobbyRepository.findByPublicId(lobbyPublicId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));
    }

    void ensureViewerIsMember(ImposterGameLobby lobby, UUID learnerId) {
        if (!imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(lobby.getId(), learnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner is not a member of this lobby");
        }
    }

    void ensureLobbyStarted(ImposterGameLobby lobby) {
        if (lobby.getStartedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby has not started yet");
        }
    }

    void ensureLobbyNotAbandoned(ImposterGameLobby lobby) {
        if (lobby.getCurrentPhase() == ImposterLobbyPhase.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby session has been abandoned");
        }
    }

    void ensurePhase(ImposterGameLobby lobby, ImposterLobbyPhase expectedPhase, String message) {
        ImposterLobbyPhase actualPhase = lobby.getCurrentPhase() == null
                ? ImposterLobbyPhase.DRAWING
                : lobby.getCurrentPhase();
        if (actualPhase != expectedPhase) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    void ensureViewerIsCurrentDrawer(ImposterGameLobby lobby, UUID learnerId) {
        if (!learnerId.equals(lobby.getCurrentDrawerLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only current drawer can perform this action");
        }
    }

    String normalizeLobbyCode(String lobbyCode) {
        if (lobbyCode == null) {
            return null;
        }

        String normalized = lobbyCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    void validateBaseVersion(Integer requestedBaseVersion, Integer currentVersion) {
        if (requestedBaseVersion != null && !requestedBaseVersion.equals(currentVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, drawingVersionConflictMessage);
        }
    }

    void validateRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be between " + min + " and " + max);
        }
    }
}
