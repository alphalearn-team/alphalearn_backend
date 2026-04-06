package com.example.demo.lessonreport;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonReportRepository extends JpaRepository<LessonReport, Long> {

    boolean existsByLesson_LessonIdAndReporterUserId(Integer lessonId, UUID reporterUserId);
}
