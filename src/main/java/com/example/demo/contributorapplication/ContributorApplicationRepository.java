package com.example.demo.contributorapplication;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributorApplicationRepository extends JpaRepository<ContributorApplication, Long> {

    boolean existsByLearner_IdAndStatus(UUID learnerId, ContributorApplicationStatus status);

    List<ContributorApplication> findAllByLearner_IdOrderBySubmittedAtDesc(UUID learnerId);
}
