package com.example.demo.quiz;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.quiz.dto.QuizAttemptResponse;
import com.example.demo.quiz.dto.SubmitQuizAttemptRequest;

@Service
public class LearnerQuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptSubmissionValidator quizAttemptSubmissionValidator;
    private final QuizAttemptScoringService quizAttemptScoringService;

    public LearnerQuizAttemptService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAttemptSubmissionValidator quizAttemptSubmissionValidator,
            QuizAttemptScoringService quizAttemptScoringService
    ) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAttemptSubmissionValidator = quizAttemptSubmissionValidator;
        this.quizAttemptScoringService = quizAttemptScoringService;
    }

    @Transactional
    public QuizAttemptResponse submitQuizAttempt(
            UUID quizPublicId,
            SubmitQuizAttemptRequest request,
            SupabaseAuthUser user
    ) {
        Learner learner = requireLearner(user);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one answer is required");
        }

        Quiz quiz = quizRepository.findByPublicId(quizPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        requirePublicLesson(quiz.getLesson());
        requireNotLessonOwner(quiz.getLesson(), user);

        List<QuizQuestion> questions = quiz.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                .toList();
        QuizAttemptSubmission submission = quizAttemptSubmissionValidator.validate(questions, request);
        int score = quizAttemptScoringService.calculateScore(questions, submission.answersByQuestionId());

        boolean isFirstAttempt = !quizAttemptRepository.existsByLearner_IdAndQuiz_QuizId(learner.getId(), quiz.getQuizId());
        OffsetDateTime attemptedAt = OffsetDateTime.now();
        QuizAttempt attempt = new QuizAttempt(
                quiz,
                learner,
                toShort(score),
                isFirstAttempt,
                attemptedAt
        );

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        return new QuizAttemptResponse(
                quiz.getPublicId(),
                savedAttempt.getAttemptedAt(),
                savedAttempt.getScore(),
                submission.totalQuestions(),
                savedAttempt.isFirstAttempt()
        );
    }

    private Learner requireLearner(SupabaseAuthUser user) {
        if (user == null || !user.isLearner() || user.learner() == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user.learner();
    }

    private void requirePublicLesson(Lesson lesson) {
        if (lesson == null
                || lesson.getDeletedAt() != null
                || lesson.getLessonModerationStatus() != LessonModerationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quiz is only available for approved lessons");
        }
    }

    private void requireNotLessonOwner(Lesson lesson, SupabaseAuthUser user) {
        if (lesson != null
                && lesson.getContributor() != null
                && lesson.getContributor().getContributorId() != null
                && user != null
                && user.userId() != null
                && lesson.getContributor().getContributorId().equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson creators cannot answer their own quiz");
        }
    }

    private short toShort(int score) {
        if (score < 0 || score > Short.MAX_VALUE) {
            throw new IllegalStateException("Quiz score is out of range for persistence.");
        }
        return (short) score;
    }
}
