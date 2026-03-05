package com.example.demo.admin.conceptsuggestion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.conceptsuggestion.ConceptSuggestion;
import com.example.demo.conceptsuggestion.ConceptSuggestionRepository;
import com.example.demo.conceptsuggestion.ConceptSuggestionStatus;
import com.example.demo.learner.Learner;

@Service
public class AdminConceptSuggestionService {

    private final ConceptSuggestionRepository conceptSuggestionRepository;

    public AdminConceptSuggestionService(ConceptSuggestionRepository conceptSuggestionRepository) {
        this.conceptSuggestionRepository = conceptSuggestionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminConceptSuggestionQueueItemDto> getSubmittedSuggestions() {
        return conceptSuggestionRepository.findAllByStatusOrderByUpdatedAtAsc(ConceptSuggestionStatus.SUBMITTED).stream()
                .map(this::toQueueItemDto)
                .toList();
    }

    private AdminConceptSuggestionQueueItemDto toQueueItemDto(ConceptSuggestion suggestion) {
        Learner owner = suggestion.getOwner();
        UUID ownerPublicId = owner == null ? null : owner.getPublicId();
        String ownerUsername = owner == null ? null : owner.getUsername();

        return new AdminConceptSuggestionQueueItemDto(
                suggestion.getPublicId(),
                suggestion.getTitle(),
                suggestion.getDescription(),
                suggestion.getStatus().name(),
                ownerPublicId,
                ownerUsername,
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt()
        );
    }
}
