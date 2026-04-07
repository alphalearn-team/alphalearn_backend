package com.example.demo.game.lobby;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.GameLobbyPhase;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GameLobbyValidationSupport {

    private final GameLobbyRepository imposterGameLobbyRepository;
    private final GameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final String drawingVersionConflictMessage;

    GameLobbyValidationSupport(
            GameLobbyRepository imposterGameLobbyRepository,
            GameLobbyMemberRepository imposterGameLobbyMemberRepository,
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

    GameLobby resolveLobbyByPublicId(UUID lobbyPublicId, boolean lockForUpdate) {
        if (lobbyPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyPublicId is required");
        }

        return (lockForUpdate
                ? imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)
                : imposterGameLobbyRepository.findByPublicId(lobbyPublicId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game lobby not found"));
    }

    void ensureViewerIsMember(GameLobby lobby, UUID learnerId) {
        if (!imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(lobby.getId(), learnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner is not a member of this lobby");
        }
    }

    void ensureLobbyStarted(GameLobby lobby) {
        if (lobby.getStartedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game lobby has not started yet");
        }
    }

    void ensureLobbyNotAbandoned(GameLobby lobby) {
        if (lobby.getCurrentPhase() == GameLobbyPhase.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game lobby session has been abandoned");
        }
    }

    void ensurePhase(GameLobby lobby, GameLobbyPhase expectedPhase, String message) {
        GameLobbyPhase actualPhase = lobby.getCurrentPhase() == null
                ? GameLobbyPhase.DRAWING
                : lobby.getCurrentPhase();
        if (actualPhase != expectedPhase) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    void ensureViewerIsCurrentDrawer(GameLobby lobby, UUID learnerId) {
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
