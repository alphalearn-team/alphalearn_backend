package com.example.demo.lessonenrollment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonEnrollmentRepository extends JpaRepository<LessonEnrollment, Integer> {

	long countBy();

}
