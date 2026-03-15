package com.example.demo.contributor;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributorRepository extends JpaRepository<Contributor, UUID> {

    Optional<Contributor> findByLearner_PublicId(UUID learnerPublicId);

    boolean existsByLearner_PublicId(UUID learnerPublicId);

    long countByDemotedAtIsNull();

    long countByDemotedAtIsNullAndPromotedAtGreaterThanEqual(OffsetDateTime promotedAt);

    long countByDemotedAtIsNullAndPromotedAtGreaterThanEqualAndPromotedAtLessThan(OffsetDateTime startInclusive, OffsetDateTime endExclusive);
}
