package com.example.demo.lessonenrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.learner.LearnerRepository;

@Service
public class LessonEnrollmentService {

    private final LessonEnrollmentRepository enrollmentRepository;
    private final LearnerRepository learnerRepository; // optional but recommended
    private final LessonEnrollmentMapper mapper;


    public LessonEnrollmentService(
            LessonEnrollmentRepository enrollmentRepository,
            LearnerRepository learnerRepository,
            LessonEnrollmentMapper mapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
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
                .orElseThrow(() -> new RuntimeException("Learner not found: " + learnerPublicId));

        return enrollmentRepository.findByLearner_PublicId(learnerPublicId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
}
