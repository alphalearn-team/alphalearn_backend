package com.example.demo.lesson.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.lesson.Lesson;

@ExtendWith(MockitoExtension.class)
class LessonListQueryServiceTest {

    @Mock
    private LessonListQueryRepository lessonListQueryRepository;

    private LessonListQueryService service;

    @BeforeEach
    void setUp() {
        service = new LessonListQueryService(lessonListQueryRepository);
    }

    @Test
    void findLessonsDelegatesToCriteriaRepository() {
        LessonListCriteria criteria = new LessonListCriteria(
                List.of(1, 2, 2),
                UUID.randomUUID(),
                null,
                LessonListAudience.CONTRIBUTOR
        );
        List<Lesson> lessons = List.of(new Lesson(), new Lesson());

        when(lessonListQueryRepository.findByCriteria(criteria)).thenReturn(lessons);

        List<Lesson> result = service.findLessons(criteria);

        assertThat(result).isSameAs(lessons);
        verify(lessonListQueryRepository).findByCriteria(criteria);
    }
}
