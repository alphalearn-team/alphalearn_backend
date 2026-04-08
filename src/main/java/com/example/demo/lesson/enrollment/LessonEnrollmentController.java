package com.example.demo.lesson.enrollment;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.enrollment.dto.LessonEnrollmentStatusDto;
import com.example.demo.lesson.enrollment.dto.LessonEnrollmentSummaryDto;
import com.example.demo.lesson.enrollment.dto.LessonProgressDto;

@RestController
@RequestMapping("/api/lesson-enrollments")
@Tag(name = "Lesson Enrollments", description = "Endpoints for lesson enrollment progress")
public class LessonEnrollmentController {

    private final LessonEnrollmentService service;

    public LessonEnrollmentController(LessonEnrollmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all enrollments (Admin)", description = "Returns all lesson_enrollments rows in the system")
    public List<LessonEnrollment> getAllEnrollments() {
        return service.getAllEnrollments();
    }

    @PostMapping("/{lessonPublicId}")
    @Operation(summary = "Enroll in a lesson", description = "Creates an enrollment for the current user in the specified lesson")
    public LessonEnrollmentStatusDto enroll(@PathVariable UUID lessonPublicId, @AuthenticationPrincipal SupabaseAuthUser user) {
        return service.enroll(lessonPublicId, user);
    }

}
