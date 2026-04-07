package com.example.demo.lesson.enrollment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.read.LessonLookupService;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.enrollment.dto.LessonEnrollmentStatusDto;
import com.example.demo.lesson.enrollment.dto.LessonEnrollmentSummaryDto;
import com.example.demo.lesson.enrollment.dto.LessonProgressDto;

@Service
public class LessonEnrollmentService {

    private final LessonEnrollmentRepository repository;
    private final LessonLookupService lessonLookupService;
    private final LearnerRepository learnerRepository;
    private final com.example.demo.quiz.QuizAttemptRepository quizAttemptRepository;
    private final com.example.demo.quiz.QuizRepository quizRepository;

    public LessonEnrollmentService(
            LessonEnrollmentRepository repository,
            LessonLookupService lessonLookupService,
            LearnerRepository learnerRepository,
            com.example.demo.quiz.QuizAttemptRepository quizAttemptRepository,
            com.example.demo.quiz.QuizRepository quizRepository
    ) {
        this.repository = repository;
        this.lessonLookupService = lessonLookupService;
        this.learnerRepository = learnerRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
    }

    public List<LessonEnrollment> getAllEnrollments() {
        return repository.findAll();
    }

    @Transactional
    public LessonEnrollmentStatusDto enroll(UUID lessonPublicId, SupabaseAuthUser user) {
        Learner learner = requireLearner(user);
        Lesson lesson = lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId);

        if (lesson.getLessonModerationStatus() != LessonModerationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only approved lessons can be enrolled in");
        }

        if (lesson.getContributor() != null && lesson.getContributor().getContributorId().equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson creators cannot enroll in their own lessons");
        }

        if (repository.existsByLearner_IdAndLesson_PublicId(learner.getId(), lessonPublicId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this lesson");
        }

        LessonEnrollment enrollment = new LessonEnrollment();
        enrollment.setLearner(learner);
        enrollment.setLesson(lesson);
        enrollment.setCompleted(false);

        repository.save(enrollment);
        return new LessonEnrollmentStatusDto(true);
    }

    @Transactional(readOnly = true)
    public LessonEnrollmentStatusDto getEnrollmentStatus(UUID lessonPublicId, SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            return new LessonEnrollmentStatusDto(false);
        }

        boolean enrolled = repository.existsByLearner_IdAndLesson_PublicId(user.userId(), lessonPublicId);
        if (!enrolled) {
            return new LessonEnrollmentStatusDto(false);
        }

        Lesson lesson = lessonLookupService.findByPublicIdOrThrow(lessonPublicId);
        return new LessonEnrollmentStatusDto(isVisibleToLearners(lesson));
    }

    @Transactional(readOnly = true)
    public List<LessonEnrollmentSummaryDto> getMyEnrollments(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            return List.of();
        }
        Learner learner = user.learner();
        return repository.findByLearner_Id(learner.getId()).stream()
                .filter(e -> isVisibleToLearners(e.getLesson()))
                .map(e -> new LessonEnrollmentSummaryDto(
                        e.getLesson().getPublicId(),
                        e.getLesson().getTitle(),
                        e.isCompleted(),
                        e.getFirstCompletedAt()
                ))
                .toList();
    }

    @Transactional
    public List<LessonProgressDto> getMyProgress(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            return List.of();
        }
        Learner learner = user.learner();
        UUID learnerId = learner.getId();

        List<LessonEnrollment> enrollments = repository.findByLearner_Id(learnerId);

        return enrollments.stream()
                .filter(enrollment -> isVisibleToLearners(enrollment.getLesson()))
                .map(enrollment -> {
            UUID lessonPublicId = enrollment.getLesson().getPublicId();
            long total = quizRepository.countByLesson_PublicId(lessonPublicId);
            long passed = quizAttemptRepository.countFullMarkQuizzesByLearnerAndLesson(learnerId, lessonPublicId);

            if (!enrollment.isCompleted() && total > 0 && passed == total) {
                enrollment.setCompleted(true);
                enrollment.setFirstCompletedAt(OffsetDateTime.now());
                repository.save(enrollment);
            }

            return new LessonProgressDto(
                lessonPublicId,
                enrollment.getLesson().getTitle(),
                enrollment.isCompleted(),
                enrollment.getFirstCompletedAt(),
                (int) total,
                (int) passed
            );
        }).toList();
    }

    @Transactional
    public void checkAndCompleteLesson(UUID learnerId, UUID lessonPublicId) {
        repository.findByLearner_IdAndLesson_PublicId(learnerId, lessonPublicId).ifPresent(enrollment -> {
            if (enrollment.isCompleted()) return;

            long total = quizRepository.countByLesson_PublicId(lessonPublicId);
            if (total == 0) return;

            long passed = quizAttemptRepository.countFullMarkQuizzesByLearnerAndLesson(learnerId, lessonPublicId);
            if (passed == total) {
                enrollment.setCompleted(true);
                enrollment.setFirstCompletedAt(OffsetDateTime.now());
                repository.save(enrollment);
            }
        });
    }

    public boolean isEnrolled(UUID learnerId, UUID lessonPublicId) {
        return repository.existsByLearner_IdAndLesson_PublicId(learnerId, lessonPublicId);
    }

    private boolean isVisibleToLearners(Lesson lesson) {
        return lesson != null
                && lesson.getDeletedAt() == null
                && lesson.getLessonModerationStatus() == LessonModerationStatus.APPROVED;
    }

    public long countEnrollments(UUID lessonPublicId) {
        return repository.countByLesson_PublicId(lessonPublicId);
    }

    public long countCompletions(UUID lessonPublicId) {
        return repository.countByLesson_PublicIdAndCompletedTrue(lessonPublicId);
    }

    private Learner requireLearner(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
        }
        // If the learner profile already exists on the auth token, use it
        if (user.learner() != null) {
            return user.learner();
        }
        // Otherwise look it up or auto-create one (just-in-time provisioning)
        return learnerRepository.findById(user.userId()).orElseGet(() -> {
            Learner newLearner = new Learner();
            newLearner.setId(user.userId());
            newLearner.setPublicId(UUID.randomUUID());
            newLearner.setUsername(null);
            newLearner.setCreatedAt(OffsetDateTime.now());
            newLearner.setTotalPoints((short) 0);
            return learnerRepository.save(newLearner);
        });
    }
}
