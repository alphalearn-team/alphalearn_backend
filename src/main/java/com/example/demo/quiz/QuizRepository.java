package com.example.demo.quiz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    Optional<Quiz> findByPublicId(UUID publicId);
    
    @Query("SELECT q FROM Quiz q WHERE q.lesson.publicId = :lessonPublicId")
    List<Quiz> findByLessonPublicId(UUID lessonPublicId);
}
