package com.example.demo.lesson.moderation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.lesson.Lesson;

public interface LessonModerationRecordRepository extends JpaRepository<LessonModerationRecord, Integer> {

    List<LessonModerationRecord> findByLessonOrderByRecordedAtDesc(Lesson lesson);

    Optional<LessonModerationRecord> findTopByLessonOrderByRecordedAtDesc(Lesson lesson);

    Optional<LessonModerationRecord> findTopByLessonAndDecisionSourceInOrderByRecordedAtDesc(
            Lesson lesson,
            Collection<LessonModerationDecisionSource> decisionSources
    );

    Optional<LessonModerationRecord> findTopByLessonAndDecisionSourceOrderByRecordedAtDesc(
            Lesson lesson,
            LessonModerationDecisionSource decisionSource
    );
}
