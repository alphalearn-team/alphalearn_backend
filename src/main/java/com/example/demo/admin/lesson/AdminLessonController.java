package com.example.demo.admin.lesson;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.query.ConceptsMatchMode;

@RestController()
@RequestMapping("/api/admin/lessons")
@Tag(name = "Admin Lessons", description = "Admin-only lesson moderation and review endpoints")
public class AdminLessonController {
    private final AdminLessonFacade adminFacade;

    public AdminLessonController(AdminLessonFacade adminFacade){
        this.adminFacade = adminFacade;
    }

    @GetMapping
    @Operation(summary = "List lessons (admin)", description = "Returns lessons with optional filters: concept IDs, conceptsMatch mode, and moderation status")
    public List<AdminLessonSummaryDto> getAllLessonsForAdmin(
            @RequestParam(required = false) List<UUID> conceptPublicIds,
            @RequestParam(defaultValue = "any") String conceptsMatch,
            @RequestParam(required = false) LessonModerationStatus status
    ) {
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return adminFacade.getAllLessons(conceptPublicIds, matchMode, status);
    }

    @GetMapping("/{lessonPublicId}")
    @Operation(summary = "Get lesson for review", description = "Returns lesson content and moderation metadata for admin review")
    public AdminLessonReviewDto getLessonForAdmin(@PathVariable UUID lessonPublicId) {
        return adminFacade.getLessonByPublicId(lessonPublicId);
    }

    @PutMapping("/{lessonPublicId}/approve")
    @Operation(summary = "Approve lesson", description = "Moves lesson moderation status from PENDING to APPROVED")
    public AdminLessonDetailDto approveLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ){
        return adminFacade.approveLesson(lessonPublicId, user);
    }

    @PutMapping("/{lessonPublicId}/reject")
    @Operation(summary = "Reject lesson", description = "Moves lesson moderation status from PENDING to REJECTED")
    public AdminLessonDetailDto rejectLesson(
            @PathVariable UUID lessonPublicId,
            @RequestBody AdminRejectLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ){
        return adminFacade.rejectLesson(lessonPublicId, request, user);
    }

}
