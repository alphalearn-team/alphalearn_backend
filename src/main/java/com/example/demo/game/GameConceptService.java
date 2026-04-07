package com.example.demo.game;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.dto.GameAssignedConceptDto;
import com.example.demo.game.dto.NextGameConceptRequest;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameConceptService {

    private final ConceptRepository conceptRepository;
    private final GameLobbyRepository imposterGameLobbyRepository;
    private final GameMonthlyPackRepository imposterMonthlyPackRepository;
    private final GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

    public GameConceptService(
            ConceptRepository conceptRepository,
            GameLobbyRepository imposterGameLobbyRepository,
            GameMonthlyPackRepository imposterMonthlyPackRepository,
            GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService
    ) {
        this.conceptRepository = conceptRepository;
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
    }

    public GameAssignedConceptDto assignNextConcept(NextGameConceptRequest request) {
        return assignNextConcept(null, request);
    }

    public GameAssignedConceptDto assignNextConcept(SupabaseAuthUser user, NextGameConceptRequest request) {
        Set<java.util.UUID> excludedConceptPublicIds = normalizeExcludedConceptPublicIds(request);
        GameLobby lobby = resolveRequestedLobby(user, request);
        if (lobby != null && lobby.getStartedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game lobby game has not started");
        }

        if (lobby != null && lobby.getConceptPoolMode() == GameLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            java.util.Optional<GameAssignedConceptDto> featuredConcept = resolveFeaturedConceptForLobby(
                    lobby,
                    excludedConceptPublicIds
            );
            if (featuredConcept.isPresent()) {
                return featuredConcept.get();
            }
        }

        List<Concept> availableConcepts = resolveCandidateConcepts(lobby)
                .stream()
                .filter(concept -> !excludedConceptPublicIds.contains(concept.getPublicId()))
                .toList();

        if (availableConcepts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No imposter game concepts are available");
        }

        Concept selectedConcept = availableConcepts.get(ThreadLocalRandom.current().nextInt(availableConcepts.size()));

        return new GameAssignedConceptDto(
                selectedConcept.getPublicId(),
                selectedConcept.getTitle()
        );
    }

    private List<Concept> resolveCandidateConcepts(GameLobby lobby) {
        if (lobby == null) {
            return conceptRepository.findAll();
        }

        if (lobby.getConceptPoolMode() == GameLobbyConceptPoolMode.FULL_CONCEPT_POOL) {
            return conceptRepository.findAll();
        }

        if (lobby.getPinnedYearMonth() == null || lobby.getPinnedYearMonth().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby monthly pack is not configured");
        }

        GameMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(lobby.getPinnedYearMonth())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Pinned monthly imposter pack is not configured: " + lobby.getPinnedYearMonth()
                ));

        return imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId())
                .stream()
                .map(row -> row.getConcept())
                .toList();
    }

    private GameLobby resolveRequestedLobby(SupabaseAuthUser user, NextGameConceptRequest request) {
        UUID lobbyPublicId = request == null ? null : request.lobbyPublicId();
        String lobbyCode = normalizeLobbyCode(request == null ? null : request.lobbyCode());
        if (lobbyPublicId == null && lobbyCode == null) {
            return null;
        }

        GameLobby lobbyByCode = null;
        if (lobbyCode != null) {
            lobbyByCode = imposterGameLobbyRepository.findByLobbyCode(lobbyCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game lobby not found"));
        }

        GameLobby lobbyByPublicId = null;
        if (lobbyPublicId != null) {
            lobbyByPublicId = imposterGameLobbyRepository.findByPublicId(lobbyPublicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game lobby not found"));
        }

        if (lobbyByCode != null && lobbyByPublicId != null && !Objects.equals(lobbyByCode.getId(), lobbyByPublicId.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode and lobbyPublicId refer to different lobbies");
        }

        GameLobby lobby = lobbyByCode != null ? lobbyByCode : lobbyByPublicId;
        return requireLobbyOwnedBy(user, lobby);
    }

    private GameLobby requireLobbyOwnedBy(SupabaseAuthUser user, GameLobby lobby) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required for lobby concepts");
        }

        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can request concepts for this private lobby");
        }

        return lobby;
    }

    private String normalizeLobbyCode(String lobbyCode) {
        if (lobbyCode == null) {
            return null;
        }
        String normalized = lobbyCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private java.util.Optional<GameAssignedConceptDto> resolveFeaturedConceptForLobby(
            GameLobby lobby,
            Set<UUID> excludedConceptPublicIds
    ) {
        if (lobby.getPinnedYearMonth() == null || lobby.getPinnedYearMonth().isBlank()) {
            return java.util.Optional.empty();
        }

        return imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept(lobby.getPinnedYearMonth())
                .filter(concept -> !excludedConceptPublicIds.contains(concept.getPublicId()))
                .map(concept -> new GameAssignedConceptDto(concept.getPublicId(), concept.getTitle()));
    }

    private Set<java.util.UUID> normalizeExcludedConceptPublicIds(NextGameConceptRequest request) {
        if (request == null || request.excludedConceptPublicIds() == null || request.excludedConceptPublicIds().isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(request.excludedConceptPublicIds());
    }
}
