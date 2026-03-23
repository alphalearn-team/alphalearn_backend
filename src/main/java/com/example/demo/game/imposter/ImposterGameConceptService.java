package com.example.demo.game.imposter;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.imposter.dto.ImposterAssignedConceptDto;
import com.example.demo.game.imposter.dto.NextImposterConceptRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class ImposterGameConceptService {

    private final ConceptRepository conceptRepository;

    public ImposterGameConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public ImposterAssignedConceptDto assignNextConcept(NextImposterConceptRequest request) {
        Set<java.util.UUID> excludedConceptPublicIds = normalizeExcludedConceptPublicIds(request);

        List<Concept> availableConcepts = conceptRepository.findAll()
                .stream()
                .filter(concept -> !excludedConceptPublicIds.contains(concept.getPublicId()))
                .toList();

        Concept selectedConcept = availableConcepts.get(ThreadLocalRandom.current().nextInt(availableConcepts.size()));

        return new ImposterAssignedConceptDto(
                selectedConcept.getPublicId(),
                selectedConcept.getTitle()
        );
    }

    private Set<java.util.UUID> normalizeExcludedConceptPublicIds(NextImposterConceptRequest request) {
        if (request == null || request.excludedConceptPublicIds() == null || request.excludedConceptPublicIds().isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(request.excludedConceptPublicIds());
    }
}
