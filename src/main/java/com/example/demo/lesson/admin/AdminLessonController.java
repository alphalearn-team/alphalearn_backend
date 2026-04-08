package com.example.demo.lesson.admin;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.LessonModerationStatus;
import com.example.demo.lesson.LessonModerationStatus;

@RestController()
@RequestMapping("/api/admin/lessons")
@Tag(name = "Admin Lessons", description = "Admin-only lesson moderation and review endpoints")
public class AdminLessonController {
    private final AdminLessonService adminLessonService;

    public AdminLessonController(AdminLessonService adminLessonService){
        this.adminLessonService = adminLessonService;
    }

    @GetMapping
    @Operation(summary = "List lessons (admin)", description = "Returns lessons with optional filters: concept IDs and moderation status")
    public List<AdminLessonSummaryDto> getAllLessonsForAdmin(
            @RequestParam(required = false) List<UUID> conceptPublicIds,
            @RequestParam(required = false) LessonModerationStatus status
    ) {
        return adminLessonService.getAllLessons(conceptPublicIds, status);
    }

    @GetMapping("/{lessonPublicId}")
    @Operation(
            summary = "Get lesson for review",
            description = "Returns lesson content together with current moderation status, latest automated moderation reasons, and the latest admin rejection reason when applicable."
    )
    public AdminLessonReviewDto getLessonForAdmin(@PathVariable UUID lessonPublicId) {
        return adminLessonService.getLessonByPublicId(lessonPublicId);
    }

    @PatchMapping("/{lessonPublicId}")
    @Operation(summary = "Moderate lesson", description = "Applies moderation action APPROVE, REJECT, or UNPUBLISH to lesson.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson moderation action applied"),
            @ApiResponse(responseCode = "400", description = "Invalid action payload"),
            @ApiResponse(responseCode = "403", description = "Authenticated admin user required"),
            @ApiResponse(responseCode = "404", description = "Lesson not found"),
            @ApiResponse(responseCode = "409", description = "Action is not valid for lesson state")
    })
    public AdminLessonDetailDto moderateLesson(
            @PathVariable UUID lessonPublicId,
            @org.springframework.web.bind.annotation.RequestBody AdminLessonModerationActionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ){
        String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase();
        return switch (action) {
            case "APPROVE" -> adminLessonService.approveLesson(lessonPublicId, user);
            case "REJECT" -> adminLessonService.rejectLesson(lessonPublicId, new AdminRejectLessonRequest(request.reason()), user);
            case "UNPUBLISH" -> adminLessonService.updateLessonModerationStatus(
                    lessonPublicId,
                    new AdminUpdateLessonModerationStatusRequest(LessonModerationStatus.UNPUBLISHED, request.resolvePendingReports()),
                    user
            );
            default -> throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported action");
        };
    }

}
