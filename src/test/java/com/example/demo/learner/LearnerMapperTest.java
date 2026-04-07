package com.example.demo.learner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.demo.learner.dto.LearnerPublicDto;

class LearnerMapperTest {

    private final LearnerMapper mapper = new LearnerMapper();

    @Test
    void toPublicDtoMapsIdentityFields() {
        UUID publicId = UUID.randomUUID();
        Learner learner = new Learner(
                UUID.randomUUID(),
                publicId,
                "learner-user",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 5
        );

        LearnerPublicDto dto = mapper.toPublicDto(learner);

        assertThat(dto.publicId()).isEqualTo(publicId);
        assertThat(dto.username()).isEqualTo("learner-user");
    }
}
