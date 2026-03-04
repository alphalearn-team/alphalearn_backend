package com.example.demo.lesson;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonSectionRepository extends JpaRepository<LessonSection, Integer> {
    
    @Query("SELECT ls FROM LessonSection ls WHERE ls.lesson.lessonId = :lessonId ORDER BY ls.orderIndex ASC")
    List<LessonSection> findByLessonIdOrderByOrderIndexAsc(@Param("lessonId") Integer lessonId);
    
    void deleteByLesson_LessonId(Integer lessonId);
}
