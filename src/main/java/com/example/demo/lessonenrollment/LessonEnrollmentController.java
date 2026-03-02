package com.example.demo.lessonenrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/lessonenrollments")
@Tag(name = "Lesson Enrollments", description = "Endpoints for lesson enrollment progress")
public class LessonEnrollmentController {

    private final LessonEnrollmentService enrollmentService;

    public LessonEnrollmentController(LessonEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @Operation(summary = "List enrollments", description = "Returns all lesson_enrollments rows currently visible to caller scope")
    public List<LessonEnrollmentPublicDTO> getEnrollments() {
        return enrollmentService.getAllEnrollments();
    }

    @GetMapping("/{learnerPublicId}/enrollments")
    public ResponseEntity<List<LessonEnrollmentPublicDTO>> getEnrollmentsByLearner(
            @PathVariable("learnerPublicId") UUID learnerPublicId
    ) {
        return ResponseEntity.ok(
                enrollmentService.getEnrollmentsByLearnerPublicId(learnerPublicId)
        );
    }
}
