package com.example.demo.learner;

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

@ExtendWith(MockitoExtension.class)
class LearnerQueryServiceTest {

    @Mock
    private LearnerRepository learnerRepository;

    private LearnerQueryService learnerQueryService;

    @BeforeEach
    void setUp() {
        learnerQueryService = new LearnerQueryService(learnerRepository, new LearnerMapper());
    }

    @Test
    void getAllPublicLearnersMapsProfilePictureUrl() {
        Learner pictured = learner("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "pictured");
        pictured.setProfilePicture("https://cdn.example.com/pictured.png");
        Learner withoutPicture = learner("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "plain");

        when(learnerRepository.findAll()).thenReturn(List.of(pictured, withoutPicture));

        var learners = learnerQueryService.getAllPublicLearners();

        assertThat(learners).hasSize(2);
        assertThat(learners.get(0).profilePictureUrl()).isEqualTo("https://cdn.example.com/pictured.png");
        assertThat(learners.get(1).profilePictureUrl()).isNull();
    }

    private Learner learner(String id, String username) {
        UUID uuid = UUID.fromString(id);
        return new Learner(
                uuid,
                UUID.randomUUID(),
                username,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
    }
}
