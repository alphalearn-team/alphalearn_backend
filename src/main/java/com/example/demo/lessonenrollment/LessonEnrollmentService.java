package com.example.demo.lessonenrollment;

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
import com.example.demo.lesson.LessonLookupService;

@Service
public class LessonEnrollmentService {

    private final LessonEnrollmentRepository enrollmentRepository;
    private final LessonLookupService lessonLookupService;
    private final LearnerRepository learnerRepository; // optional but recommended
    private final LessonEnrollmentMapper mapper;


    public LessonEnrollmentService(
            LessonEnrollmentRepository enrollmentRepository,
            LessonLookupService lessonLookupService,
            LearnerRepository learnerRepository,
            LessonEnrollmentMapper mapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.lessonLookupService = lessonLookupService;
        this.learnerRepository = learnerRepository;
        this.mapper = mapper;
    }

    public List<LessonEnrollmentPublicDTO> getAllEnrollments() {
        return enrollmentRepository.findAll()
            .stream()
            .map(mapper::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonEnrollmentPublicDTO> getEnrollmentsByLearnerPublicId(UUID learnerPublicId) {

        // Recommended: validate learner exists so you can return 404 instead of empty list
        learnerRepository.findByPublicId(learnerPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found: " + learnerPublicId));

        return enrollmentRepository.findByLearner_PublicId(learnerPublicId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonEnrollmentPublicDTO> getEnrollmentsByLearnerId(UUID learnerId) {
        return enrollmentRepository.findByLearner_Id(learnerId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

 @Transactional
    public LessonEnrollmentPublicDTO enroll(LessonEnrollmentCreateDTO dto, SupabaseAuthUser user) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        if (dto.lessonPublicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lessonPublicId is required");
        }

        UUID learnerId = user.userId();
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));

        Lesson lesson = lessonLookupService.findPublicByPublicIdOrThrow(dto.lessonPublicId());

        boolean alreadyEnrolled = enrollmentRepository
                .existsByLearner_IdAndLesson_LessonId(learner.getId(), lesson.getLessonId());

        if (alreadyEnrolled) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this lesson");
        }

        LessonEnrollment enrollment = new LessonEnrollment();
        enrollment.setLearner(learner);
        enrollment.setLesson(lesson);
        enrollment.setCompleted(false);
        enrollment.setFirstCompletedAt(null);

        LessonEnrollment saved = enrollmentRepository.save(enrollment);
        return mapper.toDTO(saved);
    }
}


    // @Transactional
    // public LessonEnrollmentPublicDTO enroll(LessonEnrollmentCreateDTO dto, SupabaseAuthUser user) {
    //     if (dto == null) {
    //         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    //     }
    //     if (user == null || user.userId() == null) {
    //         throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
    //     }

    //     UUID learnerPublicId = dto.learnerPublicId();
    //     UUID lessonPublicId = dto.lessonPublicId();

    //     if (learnerPublicId == null || lessonPublicId == null) {
    //         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "learnerPublicId and lessonPublicId are required");
    //     }

    //     // Prevent enrolling as someone else
    //     if (!learnerPublicId.equals(user.userId())) {
    //         throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot enroll on behalf of another learner");
    //     }

    //     Learner learner = learnerRepository.findByPublicId(learnerPublicId)
    //             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));

    //     // Only allow enrolling into a public/approved lesson
    //     Lesson lesson = lessonLookupService.findPublicByPublicIdOrThrow(lessonPublicId);

    //     // Duplicate prevention
    //     if (enrollmentRepository.existsByLearner_PublicIdAndLesson_PublicId(
    //             learnerPublicId,
    //             lessonPublicId
    //     )) {
    //         throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this lesson");
    //     }

    //     LessonEnrollment enrollment = new LessonEnrollment();
    //     enrollment.setLearner(learner);
    //     enrollment.setLesson(lesson);
    //     enrollment.setCompleted(false);
    //     enrollment.setFirstCompletedAt(null);

    //     LessonEnrollment saved = enrollmentRepository.save(enrollment);
    //     return mapper.toDTO(saved);
    // }

