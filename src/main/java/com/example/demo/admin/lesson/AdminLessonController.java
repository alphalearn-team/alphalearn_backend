package com.example.demo.admin.lesson;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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
    @Operation(
            summary = "Get lesson for review",
            description = "Returns lesson content together with current moderation status, latest automated moderation reasons, and the latest admin rejection reason when applicable."
    )
    public AdminLessonReviewDto getLessonForAdmin(@PathVariable UUID lessonPublicId) {
        return adminFacade.getLessonByPublicId(lessonPublicId);
    }

    @PutMapping("/{lessonPublicId}/approve")
    @Operation(summary = "Approve lesson", description = "Moves lesson moderation status from PENDING to APPROVED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson approved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Lesson not found"),
            @ApiResponse(responseCode = "409", description = "Only PENDING lessons can be approved")
    })
    public AdminLessonDetailDto approveLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ){
        return adminFacade.approveLesson(lessonPublicId, user);
    }

    @PutMapping("/{lessonPublicId}/reject")
    @Operation(
            summary = "Reject lesson",
            description = "Moves lesson moderation status from PENDING to REJECTED and stores a required admin rejection reason in moderation history."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Reject reason is missing or blank"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Lesson not found"),
            @ApiResponse(responseCode = "409", description = "Only PENDING lessons can be rejected")
    })
    public AdminLessonDetailDto rejectLesson(
            @PathVariable UUID lessonPublicId,
            @RequestBody(
                    required = true,
                    description = "Manual rejection payload containing the admin's reason.",
                    content = @Content(schema = @Schema(implementation = AdminRejectLessonRequest.class))
            )
            @org.springframework.web.bind.annotation.RequestBody AdminRejectLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ){
        return adminFacade.rejectLesson(lessonPublicId, request, user);
    }

}
