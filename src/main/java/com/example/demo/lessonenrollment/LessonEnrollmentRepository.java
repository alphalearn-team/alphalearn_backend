package com.example.demo.lessonenrollment;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonEnrollmentRepository extends JpaRepository<LessonEnrollment, Integer> {

	long countBy();

	long countByFirstCompletedAtGreaterThanEqualAndFirstCompletedAtLessThan(OffsetDateTime startInclusive, OffsetDateTime endExclusive);

}
