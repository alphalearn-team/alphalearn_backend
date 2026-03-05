package com.example.demo.lessonenrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonEnrollmentRepository extends JpaRepository<LessonEnrollment, Integer> {

    List<LessonEnrollment> findByLearner_PublicId(UUID learnerPublicId);

    boolean existsByLearner_IdAndLesson_LessonId(UUID learnerId, Integer lessonId);

    List<LessonEnrollment> findByLearner_Id(UUID learnerId);
}
