package com.example.demo.conceptsuggestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptSuggestionRepository extends JpaRepository<ConceptSuggestion, Integer> {

    Optional<ConceptSuggestion> findByPublicId(UUID publicId);

    List<ConceptSuggestion> findAllByOwner_IdOrderByUpdatedAtDesc(UUID ownerId);

    List<ConceptSuggestion> findAllByStatusOrderByUpdatedAtAsc(ConceptSuggestionStatus status);
}
