package com.example.demo.learner;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  LearnerRepository extends JpaRepository<Learner, UUID>{

    Optional<Learner> findByPublicId(UUID publicId);

    boolean existsByPublicId(UUID publicId);

    long countBy();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime startInclusive, OffsetDateTime endExclusive);
}
