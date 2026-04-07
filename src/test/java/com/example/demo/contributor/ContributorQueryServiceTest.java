package com.example.demo.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.contributor.dto.ContributorPublicDto;
import com.example.demo.learner.Learner;

@ExtendWith(MockitoExtension.class)
class ContributorQueryServiceTest {

    @Mock
    private ContributorRepository contributorRepository;

    private ContributorQueryService service;

    @BeforeEach
    void setUp() {
        service = new ContributorQueryService(contributorRepository, new ContributorMapper());
    }

    @Test
    void getAllPublicContributorsMapsRepositoryEntities() {
        Learner learner = new Learner(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "mapped-contributor",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );

        Contributor contributor = new Contributor();
        contributor.setLearner(learner);
        contributor.setPromotedAt(OffsetDateTime.parse("2026-03-02T00:00:00Z"));

        when(contributorRepository.findAll()).thenReturn(List.of(contributor));

        List<ContributorPublicDto> result = service.getAllPublicContributors();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().publicId()).isEqualTo(learner.getPublicId());
        assertThat(result.getFirst().username()).isEqualTo("mapped-contributor");
    }
}
