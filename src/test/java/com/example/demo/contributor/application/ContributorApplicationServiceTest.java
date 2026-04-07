package com.example.demo.contributor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.example.demo.contributor.application.dto.ContributorApplicationDto;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@ExtendWith(MockitoExtension.class)
class ContributorApplicationServiceTest {

    @Mock
    private ContributorApplicationRepository contributorApplicationRepository;

    @Mock
    private LearnerRepository learnerRepository;

    private ContributorApplicationService contributorApplicationService;

    @BeforeEach
    void setUp() {
        contributorApplicationService = new ContributorApplicationService(
                contributorApplicationRepository,
                learnerRepository
        );
    }

    @Test
    void submitApplicationCreatesPendingRecordForAuthenticatedLearner() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);

        when(learnerRepository.findById(learnerId)).thenReturn(Optional.of(learner));
        when(contributorApplicationRepository.existsByLearner_IdAndStatus(
                learnerId,
                ContributorApplicationStatus.PENDING
        )).thenReturn(false);
        when(contributorApplicationRepository.save(any(ContributorApplication.class))).thenAnswer(invocation -> {
            ContributorApplication application = invocation.getArgument(0);
            application.assignDefaultsIfMissing();
            return application;
        });

        ContributorApplicationDto result = contributorApplicationService.submitApplication(authUser(learnerId, learner));

        ArgumentCaptor<ContributorApplication> captor = ArgumentCaptor.forClass(ContributorApplication.class);
        verify(contributorApplicationRepository).save(captor.capture());
        ContributorApplication saved = captor.getValue();

        assertThat(saved.getLearner()).isEqualTo(learner);
        assertThat(saved.getStatus()).isEqualTo(ContributorApplicationStatus.PENDING);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.publicId()).isNotNull();
        assertThat(result.learnerPublicId()).isEqualTo(learner.getPublicId());
        assertThat(result.learnerUsername()).isEqualTo(learner.getUsername());
        assertThat(result.submittedAt()).isNotNull();
    }

    @Test
    void submitApplicationRejectsCurrentContributors() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> contributorApplicationService.submitApplication(contributorUser(learnerId, learner))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        assertThat(ex.getReason()).isEqualTo("Current contributors cannot submit contributor applications");
    }

    @Test
    void submitApplicationRejectsWhenPendingApplicationAlreadyExists() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);

        when(learnerRepository.findById(learnerId)).thenReturn(Optional.of(learner));
        when(contributorApplicationRepository.existsByLearner_IdAndStatus(
                learnerId,
                ContributorApplicationStatus.PENDING
        )).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> contributorApplicationService.submitApplication(authUser(learnerId, learner))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        assertThat(ex.getReason()).isEqualTo("A pending contributor application already exists for this learner");
    }

    @Test
    void getMyApplicationsReturnsApplicationsInRepositoryOrder() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = learner(learnerId);
        ContributorApplication newest = application(learner, ContributorApplicationStatus.PENDING, OffsetDateTime.now());
        ContributorApplication older = application(
                learner,
                ContributorApplicationStatus.REJECTED,
                OffsetDateTime.now().minusDays(1)
        );

        when(learnerRepository.existsById(learnerId)).thenReturn(true);
        when(contributorApplicationRepository.findAllByLearner_IdOrderBySubmittedAtDesc(learnerId))
                .thenReturn(List.of(newest, older));

        List<ContributorApplicationDto> result = contributorApplicationService.getMyApplications(authUser(learnerId, learner));

        assertThat(result)
                .extracting(ContributorApplicationDto::status)
                .containsExactly("PENDING", "REJECTED");
        assertThat(result)
                .extracting(ContributorApplicationDto::publicId)
                .containsExactly(newest.getPublicId(), older.getPublicId());
        assertThat(result)
                .extracting(ContributorApplicationDto::learnerPublicId)
                .containsExactly(learner.getPublicId(), learner.getPublicId());
    }

    @Test
    void getMyApplicationsRejectsUnknownLearners() {
        UUID learnerId = UUID.randomUUID();

        when(learnerRepository.existsById(learnerId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> contributorApplicationService.getMyApplications(authUser(learnerId, null))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).isEqualTo("Learner not found");
    }

    private SupabaseAuthUser authUser(UUID userId, Learner learner) {
        return new SupabaseAuthUser(userId, learner, null);
    }

    private SupabaseAuthUser contributorUser(UUID userId, Learner learner) {
        Contributor contributor = new Contributor();
        contributor.setContributorId(userId);
        contributor.setPromotedAt(OffsetDateTime.now());
        contributor.setLearner(learner);
        return new SupabaseAuthUser(userId, learner, contributor);
    }

    private Learner learner(UUID learnerId) {
        return new Learner(learnerId, UUID.randomUUID(), "user-" + learnerId, OffsetDateTime.now(), (short) 0);
    }

    private ContributorApplication application(
            Learner learner,
            ContributorApplicationStatus status,
            OffsetDateTime submittedAt
    ) {
        ContributorApplication application = new ContributorApplication();
        application.setLearner(learner);
        application.setStatus(status);
        application.setSubmittedAt(submittedAt);
        application.assignDefaultsIfMissing();
        return application;
    }
}
