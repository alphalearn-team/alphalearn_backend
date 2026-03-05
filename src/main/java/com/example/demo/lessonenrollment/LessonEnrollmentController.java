package com.example.demo.lessonenrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;

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

    @GetMapping("/me/enrollments")
    public List<LessonEnrollmentPublicDTO> getMyEnrollments(
        @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        if (user == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user required");
        }
        if (user.learner() == null) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }

        // Prefer internal learner id if you have it
        UUID learnerId = user.learner().getId(); // <-- adjust if your getter is getLearnerId()

        return enrollmentService.getEnrollmentsByLearnerId(learnerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LessonEnrollmentPublicDTO enroll(
            @RequestBody LessonEnrollmentCreateDTO dto,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return enrollmentService.enroll(dto, user);
    }
}
