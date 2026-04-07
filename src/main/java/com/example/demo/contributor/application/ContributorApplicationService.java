package com.example.demo.contributor.application;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.application.dto.ContributorApplicationDto;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@Service
public class ContributorApplicationService {

    private final ContributorApplicationRepository contributorApplicationRepository;
    private final LearnerRepository learnerRepository;

    public ContributorApplicationService(
            ContributorApplicationRepository contributorApplicationRepository,
            LearnerRepository learnerRepository
    ) {
        this.contributorApplicationRepository = contributorApplicationRepository;
        this.learnerRepository = learnerRepository;
    }

    @Transactional
    public ContributorApplicationDto submitApplication(SupabaseAuthUser user) {
        UUID learnerId = requireAuthenticatedUserId(user);
        if (user != null && user.isContributor()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Current contributors cannot submit contributor applications"
            );
        }

        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));

        if (contributorApplicationRepository.existsByLearner_IdAndStatus(learnerId, ContributorApplicationStatus.PENDING)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pending contributor application already exists for this learner"
            );
        }

        ContributorApplication application = new ContributorApplication();
        application.setLearner(learner);
        application.setStatus(ContributorApplicationStatus.PENDING);

        return toDto(contributorApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<ContributorApplicationDto> getMyApplications(SupabaseAuthUser user) {
        UUID learnerId = requireAuthenticatedUserId(user);
        if (!learnerRepository.existsById(learnerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found");
        }

        return contributorApplicationRepository.findAllByLearner_IdOrderBySubmittedAtDesc(learnerId).stream()
                .map(this::toDto)
                .toList();
    }

    private UUID requireAuthenticatedUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        return user.userId();
    }

    private ContributorApplicationDto toDto(ContributorApplication application) {
        Learner learner = application.getLearner();
        return new ContributorApplicationDto(
                application.getPublicId(),
                learner == null ? null : learner.getPublicId(),
                learner == null ? null : learner.getUsername(),
                application.getStatus().name(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}
