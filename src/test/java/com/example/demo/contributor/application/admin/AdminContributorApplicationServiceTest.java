package com.example.demo.contributor.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.contributor.Contributor;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.contributor.application.ContributorApplication;
import com.example.demo.contributor.application.ContributorApplicationRepository;
import com.example.demo.contributor.application.ContributorApplicationStatus;
import com.example.demo.contributor.application.dto.ContributorApplicationDto;
import com.example.demo.learner.Learner;
import com.example.demo.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
class AdminContributorApplicationServiceTest {

    @Mock
    private ContributorApplicationRepository contributorApplicationRepository;

    @Mock
    private ContributorRepository contributorRepository;

    @Mock
    private NotificationService notificationService;

    private AdminContributorApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AdminContributorApplicationService(contributorApplicationRepository, contributorRepository, notificationService);
    }

    @Test
    void getPendingApplicationsReturnsPendingApplicationsInRepositoryOrder() {
        Learner learner = learner(UUID.randomUUID());
        ContributorApplication first = pendingApplication(learner);
        ContributorApplication second = pendingApplication(learner);
        second.setSubmittedAt(first.getSubmittedAt().plusHours(1));

        when(contributorApplicationRepository.findAllByStatusOrderBySubmittedAtAsc(ContributorApplicationStatus.PENDING))
                .thenReturn(List.of(first, second));

        List<ContributorApplicationDto> result = service.getPendingApplications(adminUser());

        assertThat(result)
                .extracting(ContributorApplicationDto::status)
                .containsExactly("PENDING", "PENDING");
        assertThat(result)
                .extracting(ContributorApplicationDto::learnerUsername)
                .containsExactly(learner.getUsername(), learner.getUsername());
        assertThat(result)
                .extracting(ContributorApplicationDto::submittedAt)
                .containsExactly(first.getSubmittedAt(), second.getSubmittedAt());
    }

    @Test
    void getApplicationByPublicIdReturnsApplication() {
        UUID applicationPublicId = UUID.randomUUID();
        Learner learner = learner(UUID.randomUUID());
        ContributorApplication application = pendingApplication(learner);

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.of(application));

        ContributorApplicationDto dto = service.getApplicationByPublicId(applicationPublicId, adminUser());

        assertThat(dto.status()).isEqualTo("PENDING");
        assertThat(dto.learnerPublicId()).isEqualTo(learner.getPublicId());
        assertThat(dto.learnerUsername()).isEqualTo(learner.getUsername());
        assertThat(dto.submittedAt()).isEqualTo(application.getSubmittedAt());
    }

    @Test
    void getApplicationByPublicIdRejectsUnknownApplication() {
        UUID applicationPublicId = UUID.randomUUID();

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getApplicationByPublicId(applicationPublicId, adminUser())
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("Contributor application not found");
    }

    @Test
    void approveApplicationApprovesPendingAndPromotesLearnerWithoutContributorRecord() {
        UUID applicationPublicId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);
        ContributorApplication application = pendingApplication(learner);

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.of(application));
        when(contributorRepository.findById(learnerId)).thenReturn(Optional.empty());
        when(contributorApplicationRepository.save(any(ContributorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContributorApplicationDto dto = service.approveApplication(applicationPublicId, adminUser());

        ArgumentCaptor<Contributor> contributorCaptor = ArgumentCaptor.forClass(Contributor.class);
        verify(contributorRepository).save(contributorCaptor.capture());
        assertThat(contributorCaptor.getValue().getLearner()).isEqualTo(learner);
        assertThat(contributorCaptor.getValue().getDemotedAt()).isNull();

        assertThat(dto.status()).isEqualTo("APPROVED");
        assertThat(dto.reviewedAt()).isNotNull();
        assertThat(dto.rejectionReason()).isNull();
    }

    @Test
    void approveApplicationReactivatesDemotedContributor() {
        UUID applicationPublicId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);
        ContributorApplication application = pendingApplication(learner);
        Contributor contributor = new Contributor();
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.now().minusDays(10));
        contributor.setDemotedAt(OffsetDateTime.now().minusDays(1));

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.of(application));
        when(contributorRepository.findById(learnerId)).thenReturn(Optional.of(contributor));
        when(contributorApplicationRepository.save(any(ContributorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approveApplication(applicationPublicId, adminUser());

        verify(contributorRepository).save(contributor);
        assertThat(contributor.getDemotedAt()).isNull();
        assertThat(contributor.isCurrentContributor()).isTrue();
    }

    @Test
    void approveApplicationDoesNotRewriteCurrentContributor() {
        UUID applicationPublicId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);
        ContributorApplication application = pendingApplication(learner);
        Contributor contributor = new Contributor();
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.now().minusDays(1));
        contributor.setDemotedAt(null);

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.of(application));
        when(contributorRepository.findById(learnerId)).thenReturn(Optional.of(contributor));
        when(contributorApplicationRepository.save(any(ContributorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approveApplication(applicationPublicId, adminUser());

        verify(contributorRepository, never()).save(contributor);
    }

    @Test
    void rejectApplicationRejectsPendingWithReason() {
        UUID applicationPublicId = UUID.randomUUID();
        Learner learner = learner(UUID.randomUUID());
        ContributorApplication application = pendingApplication(learner);

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.of(application));
        when(contributorApplicationRepository.save(any(ContributorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContributorApplicationDto dto = service.rejectApplication(
                applicationPublicId,
                new RejectContributorApplicationRequest("Need more activity"),
                adminUser()
        );

        verify(contributorRepository, never()).save(any(Contributor.class));
        assertThat(dto.status()).isEqualTo("REJECTED");
        assertThat(dto.reviewedAt()).isNotNull();
        assertThat(dto.rejectionReason()).isEqualTo("Need more activity");
    }

    @Test
    void approveApplicationRejectsNonPendingStatus() {
        UUID applicationPublicId = UUID.randomUUID();
        Learner learner = learner(UUID.randomUUID());
        ContributorApplication application = pendingApplication(learner);
        application.setStatus(ContributorApplicationStatus.REJECTED);

        when(contributorApplicationRepository.findByPublicId(applicationPublicId)).thenReturn(Optional.of(application));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.approveApplication(applicationPublicId, adminUser())
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        assertThat(ex.getReason()).isEqualTo("Only PENDING contributor applications can be approved");
    }

    @Test
    void rejectApplicationRequiresReason() {
        UUID applicationPublicId = UUID.randomUUID();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.rejectApplication(
                        applicationPublicId,
                        new RejectContributorApplicationRequest("   "),
                        adminUser()
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("Reject reason is required");
    }

    private SupabaseAuthUser adminUser() {
        return new SupabaseAuthUser(UUID.randomUUID(), null, null);
    }

    private Learner learner(UUID learnerId) {
        return new Learner(learnerId, UUID.randomUUID(), "user-" + learnerId, OffsetDateTime.now(), (short) 0);
    }

    private ContributorApplication pendingApplication(Learner learner) {
        ContributorApplication application = new ContributorApplication();
        application.setLearner(learner);
        application.setStatus(ContributorApplicationStatus.PENDING);
        application.setSubmittedAt(OffsetDateTime.now().minusDays(1));
        application.setReviewedAt(null);
        application.setRejectionReason(null);
        return application;
    }
}
