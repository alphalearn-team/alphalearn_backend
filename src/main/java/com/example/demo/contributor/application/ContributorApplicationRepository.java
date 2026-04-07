package com.example.demo.contributor.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributorApplicationRepository extends JpaRepository<ContributorApplication, Long> {

    Optional<ContributorApplication> findByPublicId(UUID publicId);

    boolean existsByLearner_IdAndStatus(UUID learnerId, ContributorApplicationStatus status);

    List<ContributorApplication> findAllByLearner_IdOrderBySubmittedAtDesc(UUID learnerId);

    List<ContributorApplication> findAllByStatusOrderBySubmittedAtAsc(ContributorApplicationStatus status);
}
