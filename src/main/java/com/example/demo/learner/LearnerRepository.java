package com.example.demo.learner;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  LearnerRepository extends JpaRepository<Learner, UUID>{

    Optional<Learner> findByPublicId(UUID publicId);

    List<Learner> findAllByPublicIdIn(Collection<UUID> publicIds);
    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByPublicId(UUID publicId);

    long countBy();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime startInclusive, OffsetDateTime endExclusive);
}
