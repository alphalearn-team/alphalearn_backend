package com.example.demo.admin.contributorapplication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.contributorapplication.ContributorApplication;
import com.example.demo.contributorapplication.ContributorApplicationRepository;
import com.example.demo.contributorapplication.ContributorApplicationStatus;
import com.example.demo.contributorapplication.dto.ContributorApplicationDto;
import com.example.demo.learner.Learner;

@Service
public class AdminContributorApplicationService {

    private final ContributorApplicationRepository contributorApplicationRepository;
    private final ContributorRepository contributorRepository;

    public AdminContributorApplicationService(
            ContributorApplicationRepository contributorApplicationRepository,
            ContributorRepository contributorRepository
    ) {
        this.contributorApplicationRepository = contributorApplicationRepository;
        this.contributorRepository = contributorRepository;
    }

    @Transactional(readOnly = true)
    public List<ContributorApplicationDto> getPendingApplications(SupabaseAuthUser user) {
        requireActorUserId(user);

        return contributorApplicationRepository
                .findAllByStatusOrderBySubmittedAtAsc(ContributorApplicationStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContributorApplicationDto getApplicationByPublicId(UUID applicationPublicId, SupabaseAuthUser user) {
        requireActorUserId(user);

        ContributorApplication application = contributorApplicationRepository.findByPublicId(applicationPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor application not found"));

        return toDto(application);
    }

    @Transactional
    public ContributorApplicationDto approveApplication(UUID applicationPublicId, SupabaseAuthUser user) {
        requireActorUserId(user);

        ContributorApplication application = contributorApplicationRepository.findByPublicId(applicationPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor application not found"));

        requirePendingApplication(application, "approved");

        OffsetDateTime reviewedAt = OffsetDateTime.now();
        application.setStatus(ContributorApplicationStatus.APPROVED);
        application.setReviewedAt(reviewedAt);
        application.setRejectionReason(null);

        Learner learner = application.getLearner();
        if (learner == null || learner.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contributor application learner is missing");
        }
        ensureCurrentContributor(learner, reviewedAt);

        ContributorApplication saved = contributorApplicationRepository.save(application);
        return toDto(saved);
    }

    @Transactional
    public ContributorApplicationDto rejectApplication(
            UUID applicationPublicId,
            RejectContributorApplicationRequest request,
            SupabaseAuthUser user
    ) {
        requireActorUserId(user);

        String reason = trimToNull(request == null ? null : request.reason());
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reject reason is required");
        }

        ContributorApplication application = contributorApplicationRepository.findByPublicId(applicationPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor application not found"));

        requirePendingApplication(application, "rejected");

        application.setStatus(ContributorApplicationStatus.REJECTED);
        application.setReviewedAt(OffsetDateTime.now());
        application.setRejectionReason(reason);

        ContributorApplication saved = contributorApplicationRepository.save(application);
        return toDto(saved);
    }

    private void ensureCurrentContributor(Learner learner, OffsetDateTime promotedAt) {
        Contributor contributor = contributorRepository.findById(learner.getId()).orElse(null);
        if (contributor == null) {
            contributorRepository.save(new Contributor(learner, promotedAt));
            return;
        }

        if (contributor.isCurrentContributor()) {
            return;
        }

        contributor.setPromotedAt(promotedAt);
        contributor.setDemotedAt(null);
        contributorRepository.save(contributor);
    }

    private void requirePendingApplication(ContributorApplication application, String action) {
        if (application.getStatus() != ContributorApplicationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only PENDING contributor applications can be " + action
            );
        }
    }

    private UUID requireActorUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated admin user required");
        }
        return user.userId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ContributorApplicationDto toDto(ContributorApplication application) {
        return new ContributorApplicationDto(
                application.getPublicId(),
                application.getStatus().name(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}
