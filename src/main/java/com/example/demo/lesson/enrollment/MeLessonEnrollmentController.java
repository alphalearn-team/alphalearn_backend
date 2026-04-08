package com.example.demo.lesson.enrollment;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.enrollment.dto.LessonEnrollmentStatusDto;
import com.example.demo.lesson.enrollment.dto.LessonEnrollmentSummaryDto;
import com.example.demo.lesson.enrollment.dto.LessonProgressDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "My Lesson Enrollments", description = "Current learner lesson enrollment endpoints")
public class MeLessonEnrollmentController {

    private final LessonEnrollmentService lessonEnrollmentService;

    public MeLessonEnrollmentController(LessonEnrollmentService lessonEnrollmentService) {
        this.lessonEnrollmentService = lessonEnrollmentService;
    }

    @GetMapping("/lesson-enrollments")
    @Operation(summary = "List my enrollments", description = "Returns all lessons the current user is enrolled in")
    public List<LessonEnrollmentSummaryDto> getMyEnrollments(@AuthenticationPrincipal SupabaseAuthUser user) {
        return lessonEnrollmentService.getMyEnrollments(user);
    }

    @GetMapping("/lesson-enrollments/{lessonPublicId}")
    @Operation(summary = "Check enrollment status", description = "Returns whether the current user is enrolled in the specified lesson")
    public LessonEnrollmentStatusDto getEnrollmentStatus(@PathVariable UUID lessonPublicId, @AuthenticationPrincipal SupabaseAuthUser user) {
        return lessonEnrollmentService.getEnrollmentStatus(lessonPublicId, user);
    }

    @GetMapping(value = "/lesson-enrollments", params = "view=PROGRESS")
    @Operation(summary = "My lesson progress", description = "Returns progress for all enrolled lessons, including passed/total quiz counts and completion status.")
    public List<LessonProgressDto> getMyProgress(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam String view
    ) {
        return lessonEnrollmentService.getMyProgress(user);
    }
}
