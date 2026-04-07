package com.example.demo.contributor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.demo.contributor.dto.ContributorPublicDto;
import com.example.demo.learner.Learner;

class ContributorMapperTest {

    private final ContributorMapper mapper = new ContributorMapper();

    @Test
    void toPublicDtoMapsLearnerFieldsWhenLearnerExists() {
        Learner learner = new Learner(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "contrib-user",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 10
        );
        OffsetDateTime promotedAt = OffsetDateTime.parse("2026-03-02T00:00:00Z");
        OffsetDateTime demotedAt = OffsetDateTime.parse("2026-03-03T00:00:00Z");

        Contributor contributor = new Contributor();
        contributor.setLearner(learner);
        contributor.setPromotedAt(promotedAt);
        contributor.setDemotedAt(demotedAt);

        ContributorPublicDto dto = mapper.toPublicDto(contributor);

        assertThat(dto.publicId()).isEqualTo(learner.getPublicId());
        assertThat(dto.username()).isEqualTo("contrib-user");
        assertThat(dto.promotedAt()).isEqualTo(promotedAt);
        assertThat(dto.demotedAt()).isEqualTo(demotedAt);
    }

    @Test
    void toPublicDtoReturnsNullIdentityFieldsWhenLearnerMissing() {
        Contributor contributor = new Contributor();
        contributor.setLearner(null);
        contributor.setPromotedAt(OffsetDateTime.parse("2026-03-02T00:00:00Z"));

        ContributorPublicDto dto = mapper.toPublicDto(contributor);

        assertThat(dto.publicId()).isNull();
        assertThat(dto.username()).isNull();
        assertThat(dto.promotedAt()).isEqualTo(OffsetDateTime.parse("2026-03-02T00:00:00Z"));
    }
}
