package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LessonConceptSupport {

    private final ConceptRepository conceptRepository;

    LessonConceptSupport(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    List<UUID> normalizeCreateConceptPublicIds(List<UUID> rawConceptPublicIds) {
        if (rawConceptPublicIds == null) {
            return List.of();
        }

        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        for (UUID value : rawConceptPublicIds) {
            if (value != null) {
                ids.add(value);
            }
        }
        return List.copyOf(ids);
    }

    List<Integer> resolveConceptIdsByPublicIds(List<UUID> conceptPublicIds) {
        if (conceptPublicIds == null || conceptPublicIds.isEmpty()) {
            return List.of();
        }

        List<UUID> normalizedPublicIds = conceptPublicIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedPublicIds.isEmpty()) {
            return List.of();
        }

        List<Concept> concepts = conceptRepository.findAllByPublicIdIn(normalizedPublicIds);
        ensureAllConceptsResolved(concepts, normalizedPublicIds);
        return concepts.stream().map(Concept::getConceptId).toList();
    }

    List<Concept> resolveConceptEntities(List<UUID> conceptPublicIds) {
        List<Concept> concepts = conceptRepository.findAllByPublicIdIn(conceptPublicIds);
        ensureAllConceptsResolved(concepts, conceptPublicIds);
        return concepts;
    }

    private void ensureAllConceptsResolved(List<Concept> concepts, List<UUID> expectedPublicIds) {
        if (concepts.size() != expectedPublicIds.size()) {
            Set<UUID> foundPublicIds = concepts.stream()
                    .map(Concept::getPublicId)
                    .collect(java.util.stream.Collectors.toSet());
            UUID missingPublicId = expectedPublicIds.stream()
                    .filter(id -> !foundPublicIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Concept not found" + (missingPublicId == null ? "" : ": " + missingPublicId));
        }
    }
}
