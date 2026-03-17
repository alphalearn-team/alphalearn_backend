package com.example.demo.quiz;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {

    boolean existsByLearner_IdAndQuiz_QuizId(UUID learnerId, Integer quizId);

    Optional<QuizAttempt> findFirstByLearner_IdAndQuiz_QuizIdOrderByAttemptedAtDescAttemptIdDesc(UUID learnerId, Integer quizId);
}
