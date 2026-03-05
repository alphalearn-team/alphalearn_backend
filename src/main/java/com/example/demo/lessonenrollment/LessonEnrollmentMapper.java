package com.example.demo.lessonenrollment;

import org.springframework.stereotype.Component;

@Component
public class LessonEnrollmentMapper {

    public LessonEnrollmentPublicDTO toDTO(LessonEnrollment lessonEnrollment) {
        return new LessonEnrollmentPublicDTO(
                lessonEnrollment.getEnrollmentId(),
                lessonEnrollment.getLearner().getPublicId(),
                lessonEnrollment.getLesson().getPublicId(),
                lessonEnrollment.getLesson().getLessonModerationStatus().name(),
                lessonEnrollment.isCompleted(),
                lessonEnrollment.getFirstCompletedAt()
        );
    }
}
