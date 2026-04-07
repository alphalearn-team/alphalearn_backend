package com.example.demo.learner.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerMapper;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.learner.dto.LearnerPublicDto;

@ExtendWith(MockitoExtension.class)
class LearnerQueryServiceTest {

    @Mock
    private LearnerRepository learnerRepository;

    private LearnerQueryService service;

    @BeforeEach
    void setUp() {
        service = new LearnerQueryService(learnerRepository, new LearnerMapper());
    }

    @Test
    void getAllLearnersReturnsRepositoryValues() {
        List<Learner> learners = List.of(new Learner(), new Learner());
        when(learnerRepository.findAll()).thenReturn(learners);

        List<Learner> result = service.getAllLearners();

        assertThat(result).isSameAs(learners);
        verify(learnerRepository).findAll();
    }

    @Test
    void getAllPublicLearnersMapsToPublicDtos() {
        Learner learner = new Learner(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "public-learner",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        when(learnerRepository.findAll()).thenReturn(List.of(learner));

        List<LearnerPublicDto> result = service.getAllPublicLearners();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().publicId()).isEqualTo(learner.getPublicId());
        assertThat(result.getFirst().username()).isEqualTo("public-learner");
    }
}
