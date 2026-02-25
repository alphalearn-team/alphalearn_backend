package com.example.demo.concept;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Integer>{

    Optional<Concept> findByPublicId(UUID conceptPublicId);

    java.util.List<Concept> findAllByPublicIdIn(java.util.Collection<UUID> publicIds);

    boolean existsByPublicId(UUID conceptPublicId);
}
