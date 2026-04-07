package com.example.demo.quiz;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {

    boolean existsByLearner_IdAndQuiz_QuizId(UUID learnerId, Integer quizId);

    Optional<QuizAttempt> findFirstByLearner_IdAndQuiz_QuizIdOrderByAttemptedAtDescAttemptIdDesc(UUID learnerId, Integer quizId);

    Optional<QuizAttempt> findFirstByLearner_IdAndQuiz_QuizIdOrderByScoreDescAttemptedAtDescAttemptIdDesc(
            UUID learnerId,
            Integer quizId
    );

    // Best score a learner has ever achieved for a specific quiz (by publicId)
    @Query("SELECT MAX(a.score) FROM QuizAttempt a WHERE a.learner.id = :learnerId AND a.quiz.publicId = :quizPublicId")
    Optional<Integer> findBestScore(@Param("learnerId") UUID learnerId, @Param("quizPublicId") UUID quizPublicId);

    // Count how many distinct quizzes in a lesson the learner has passed at full marks.
    // A quiz is "passed" when the learner's best score equals the count of questions in that quiz.
    @Query("""
        SELECT COUNT(DISTINCT a.quiz.quizId)
        FROM QuizAttempt a
        WHERE a.learner.id = :learnerId
          AND a.quiz.lesson.publicId = :lessonPublicId
          AND a.score = (
              SELECT COUNT(q) FROM QuizQuestion q WHERE q.quiz = a.quiz
          )
        """)
    long countFullMarkQuizzesByLearnerAndLesson(
            @Param("learnerId") UUID learnerId,
            @Param("lessonPublicId") UUID lessonPublicId
    );
}
