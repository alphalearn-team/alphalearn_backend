package com.example.demo.lessonenrollment;

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
import com.example.demo.lessonenrollment.dto.LessonEnrollmentStatusDto;
import com.example.demo.lessonenrollment.dto.LessonEnrollmentSummaryDto;
import com.example.demo.lessonenrollment.dto.LessonProgressDto;

@RestController
@RequestMapping("/api/lessonenrollments")
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

    @GetMapping("/me")
    @Operation(summary = "List my enrollments", description = "Returns all lessons the current user is enrolled in")
    public List<LessonEnrollmentSummaryDto> getMyEnrollments(@AuthenticationPrincipal SupabaseAuthUser user) {
        return service.getMyEnrollments(user);
    }

    @GetMapping("/me/{lessonPublicId}")
    @Operation(summary = "Check enrollment status", description = "Returns whether the current user is enrolled in the specified lesson")
    public LessonEnrollmentStatusDto getEnrollmentStatus(@PathVariable UUID lessonPublicId, @AuthenticationPrincipal SupabaseAuthUser user) {
        return service.getEnrollmentStatus(lessonPublicId, user);
    }

    @GetMapping("/me/progress")
    @Operation(summary = "My lesson progress", description = "Returns progress for all enrolled lessons, including passed/total quiz counts and completion status.")
    public List<LessonProgressDto> getMyProgress(@AuthenticationPrincipal SupabaseAuthUser user) {
        return service.getMyProgress(user);
    }
}
