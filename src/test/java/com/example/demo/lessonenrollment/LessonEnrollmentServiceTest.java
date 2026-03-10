package com.example.demo.lessonenrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.learner.LearnerRepository;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;

@ExtendWith(MockitoExtension.class)
class LessonEnrollmentServiceTest {

    @Mock
    private LessonEnrollmentRepository enrollmentRepository;

    @Mock
    private LessonLookupService lessonLookupService;

    @Mock
    private LearnerRepository learnerRepository;

    private final LessonEnrollmentMapper mapper = new LessonEnrollmentMapper();

    @Test
    void getAllEnrollmentsMapsRepositoryResultsToPublicDtos() {
        LessonEnrollmentService service = new LessonEnrollmentService(
                enrollmentRepository,
                lessonLookupService,
                learnerRepository,
                mapper
        );
        LessonEnrollment first = enrollment(1, true);
        LessonEnrollment second = enrollment(2, false);

        when(enrollmentRepository.findAll()).thenReturn(List.of(first, second));

        List<LessonEnrollmentPublicDTO> result = service.getAllEnrollments();

        assertThat(result).containsExactly(
                new LessonEnrollmentPublicDTO(
                        1,
                        first.getLearner().getPublicId(),
                        first.getLesson().getPublicId(),
                        "APPROVED",
                        true,
                        first.getFirstCompletedAt()
                ),
                new LessonEnrollmentPublicDTO(
                        2,
                        second.getLearner().getPublicId(),
                        second.getLesson().getPublicId(),
                        "APPROVED",
                        false,
                        null
                )
        );
        verify(enrollmentRepository).findAll();
    }

    private LessonEnrollment enrollment(int id, boolean completed) {
        Learner learner = new Learner(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "learner-" + id,
                OffsetDateTime.now(),
                (short) 0
        );
        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson " + id);
        lesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
        ReflectionTestUtils.setField(lesson, "publicId", UUID.randomUUID());

        return new LessonEnrollment(
                id,
                learner,
                lesson,
                completed,
                completed ? OffsetDateTime.now() : null
        );
    }
}
