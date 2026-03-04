package com.example.demo.conceptsuggestion;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.conceptsuggestion.dto.ConceptSuggestionDto;
import com.example.demo.conceptsuggestion.dto.SaveConceptSuggestionRequest;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@Service
public class ConceptSuggestionService {

    private final ConceptSuggestionRepository conceptSuggestionRepository;
    private final LearnerRepository learnerRepository;

    public ConceptSuggestionService(
            ConceptSuggestionRepository conceptSuggestionRepository,
            LearnerRepository learnerRepository
    ) {
        this.conceptSuggestionRepository = conceptSuggestionRepository;
        this.learnerRepository = learnerRepository;
    }

    @Transactional
    public ConceptSuggestionDto createDraft(SaveConceptSuggestionRequest request, SupabaseAuthUser user) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        Learner owner = requireOwnerLearner(user);

        ConceptSuggestion suggestion = new ConceptSuggestion();
        suggestion.setOwner(owner);
        suggestion.setTitle(trimToNull(request.title()));
        suggestion.setDescription(trimToNull(request.description()));

        return toDto(conceptSuggestionRepository.save(suggestion));
    }

    @Transactional(readOnly = true)
    public List<ConceptSuggestionDto> getMyDrafts(SupabaseAuthUser user) {
        UUID ownerId = requireAuthenticatedUserId(user);
        return conceptSuggestionRepository.findAllByOwner_IdOrderByUpdatedAtDesc(ownerId).stream()
                .filter(suggestion -> suggestion.getStatus() == ConceptSuggestionStatus.DRAFT)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConceptSuggestionDto getSuggestion(UUID conceptSuggestionPublicId, SupabaseAuthUser user) {
        UUID ownerId = requireAuthenticatedUserId(user);
        ConceptSuggestion suggestion = conceptSuggestionRepository.findByPublicId(conceptSuggestionPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept suggestion not found"));

        requireOwner(suggestion, ownerId);
        return toDto(suggestion);
    }

    @Transactional
    public ConceptSuggestionDto updateDraft(
            UUID conceptSuggestionPublicId,
            SaveConceptSuggestionRequest request,
            SupabaseAuthUser user
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        UUID ownerId = requireAuthenticatedUserId(user);
        ConceptSuggestion suggestion = conceptSuggestionRepository.findByPublicId(conceptSuggestionPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept suggestion not found"));

        requireOwner(suggestion, ownerId);
        if (suggestion.getStatus() != ConceptSuggestionStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft concept suggestions can be edited");
        }

        suggestion.setTitle(trimToNull(request.title()));
        suggestion.setDescription(trimToNull(request.description()));

        return toDto(conceptSuggestionRepository.save(suggestion));
    }

    private Learner requireOwnerLearner(SupabaseAuthUser user) {
        UUID userId = requireAuthenticatedUserId(user);
        return learnerRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));
    }

    private UUID requireAuthenticatedUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        return user.userId();
    }

    private void requireOwner(ConceptSuggestion suggestion, UUID ownerId) {
        UUID suggestionOwnerId = suggestion.getOwner() == null ? null : suggestion.getOwner().getId();
        if (suggestionOwnerId == null || !suggestionOwnerId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Concept suggestion owner access required");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ConceptSuggestionDto toDto(ConceptSuggestion suggestion) {
        return new ConceptSuggestionDto(
                suggestion.getPublicId(),
                suggestion.getTitle(),
                suggestion.getDescription(),
                suggestion.getStatus().name(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt()
        );
    }
}
