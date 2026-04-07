package com.example.demo.lesson.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class LessonListQueryRepositoryTest {

    private final LessonListQueryRepository repository = new LessonListQueryRepository();

    @Test
    void findByCriteriaReturnsEmptyForContributorAudienceWithoutContributorId() {
        LessonListCriteria criteria = new LessonListCriteria(
                List.of(1, 2, 2),
                null,
                null,
                LessonListAudience.CONTRIBUTOR
        );

        assertThat(repository.findByCriteria(criteria)).isEmpty();
    }
}
