package com.example.demo.lessonenrollment;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lessonenrollments")
@Tag(name = "Lesson Enrollments", description = "Endpoints for lesson enrollment progress")
public class LessonEnrollmentController {

    private final LessonEnrollmentService service;

    public LessonEnrollmentController(LessonEnrollmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List enrollments", description = "Returns all lesson_enrollments rows currently visible to caller scope")
    public List<LessonEnrollment> getEnrollments() {
        return service.getAllEnrollments();
    }
}
