package com.example.demo.lessonenrollment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonEnrollmentRepository extends JpaRepository<LessonEnrollment, Integer> {

	long countBy();

	long countByFirstCompletedAtGreaterThanEqualAndFirstCompletedAtLessThan(OffsetDateTime startInclusive, OffsetDateTime endExclusive);

	List<LessonEnrollment> findByLearner_Id(UUID learnerId);

	boolean existsByLearner_IdAndLesson_PublicId(UUID learnerId, UUID lessonPublicId);

	Optional<LessonEnrollment> findByLearner_IdAndLesson_PublicId(UUID learnerId, UUID lessonPublicId);

	long countByLesson_PublicId(UUID lessonPublicId);

	long countByLesson_PublicIdAndCompletedTrue(UUID lessonPublicId);
}
