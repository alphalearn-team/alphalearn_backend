package com.example.demo.contributor;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributorRepository extends JpaRepository<Contributor, UUID> {

    Optional<Contributor> findByLearner_PublicId(UUID publicId);

    boolean existsByLearner_PublicId(UUID publicId);
}
