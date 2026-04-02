package com.example.demo.game.imposter;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.dto.ImposterAssignedConceptDto;
import com.example.demo.game.imposter.dto.NextImposterConceptRequest;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImposterGameConceptService {

    private final ConceptRepository conceptRepository;
    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

    public ImposterGameConceptService(
            ConceptRepository conceptRepository,
            ImposterGameLobbyRepository imposterGameLobbyRepository,
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService
    ) {
        this.conceptRepository = conceptRepository;
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.imposterWeeklyFeaturedConceptService = imposterWeeklyFeaturedConceptService;
    }

    public ImposterAssignedConceptDto assignNextConcept(NextImposterConceptRequest request) {
        return assignNextConcept(null, request);
    }

    public ImposterAssignedConceptDto assignNextConcept(SupabaseAuthUser user, NextImposterConceptRequest request) {
        Set<java.util.UUID> excludedConceptPublicIds = normalizeExcludedConceptPublicIds(request);
        UUID lobbyPublicId = request == null ? null : request.lobbyPublicId();

        if (lobbyPublicId != null) {
            ImposterGameLobby lobby = requireLobbyOwnedBy(user, lobbyPublicId);
            if (lobby.getConceptPoolMode() == ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
                java.util.Optional<ImposterAssignedConceptDto> featuredConcept = resolveFeaturedConceptForLobby(
                        lobby,
                        excludedConceptPublicIds
                );
                if (featuredConcept.isPresent()) {
                    return featuredConcept.get();
                }
            }
        }

        List<Concept> availableConcepts = resolveCandidateConcepts(user, request)
                .stream()
                .filter(concept -> !excludedConceptPublicIds.contains(concept.getPublicId()))
                .toList();

        if (availableConcepts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No imposter game concepts are available");
        }

        Concept selectedConcept = availableConcepts.get(ThreadLocalRandom.current().nextInt(availableConcepts.size()));

        return new ImposterAssignedConceptDto(
                selectedConcept.getPublicId(),
                selectedConcept.getTitle()
        );
    }

    private List<Concept> resolveCandidateConcepts(SupabaseAuthUser user, NextImposterConceptRequest request) {
        UUID lobbyPublicId = request == null ? null : request.lobbyPublicId();
        if (lobbyPublicId == null) {
            return conceptRepository.findAll();
        }

        ImposterGameLobby lobby = requireLobbyOwnedBy(user, lobbyPublicId);

        if (lobby.getConceptPoolMode() == ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL) {
            return conceptRepository.findAll();
        }

        if (lobby.getPinnedYearMonth() == null || lobby.getPinnedYearMonth().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby monthly pack is not configured");
        }

        ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(lobby.getPinnedYearMonth())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Pinned monthly imposter pack is not configured: " + lobby.getPinnedYearMonth()
                ));

        return imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId())
                .stream()
                .map(row -> row.getConcept())
                .toList();
    }

    private ImposterGameLobby requireLobbyOwnedBy(SupabaseAuthUser user, UUID lobbyPublicId) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required for lobby concepts");
        }

        ImposterGameLobby lobby = imposterGameLobbyRepository.findByPublicId(lobbyPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));

        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can request concepts for this private lobby");
        }

        return lobby;
    }

    private java.util.Optional<ImposterAssignedConceptDto> resolveFeaturedConceptForLobby(
            ImposterGameLobby lobby,
            Set<UUID> excludedConceptPublicIds
    ) {
        if (lobby.getPinnedYearMonth() == null || lobby.getPinnedYearMonth().isBlank()) {
            return java.util.Optional.empty();
        }

        return imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept(lobby.getPinnedYearMonth())
                .filter(concept -> !excludedConceptPublicIds.contains(concept.getPublicId()))
                .map(concept -> new ImposterAssignedConceptDto(concept.getPublicId(), concept.getTitle()));
    }

    private Set<java.util.UUID> normalizeExcludedConceptPublicIds(NextImposterConceptRequest request) {
        if (request == null || request.excludedConceptPublicIds() == null || request.excludedConceptPublicIds().isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(request.excludedConceptPublicIds());
    }
}
