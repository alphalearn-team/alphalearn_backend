package com.example.demo.lessonenrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonEnrollmentRepository extends JpaRepository<LessonEnrollment, Integer> {

    // Uses JPA property navigation: enrollment.learner.publicId
    List<LessonEnrollment> findByLearner_PublicId(UUID learnerPublicId);

}
