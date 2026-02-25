package com.example.demo.concept;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Integer>{

    Optional<Concept> findByPublicId(UUID publicId);

    boolean existsByPublicId(UUID publicId);
}
