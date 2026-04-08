package com.example.demo.lesson.read;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.lesson.dto.CreateLessonRequest;
import com.example.demo.lesson.dto.UpdateLessonRequest;
import com.example.demo.lesson.dto.LessonContributorSummaryDto;
import com.example.demo.lesson.dto.LessonDetailDto;
import com.example.demo.lesson.dto.LessonDetailView;
import com.example.demo.lesson.dto.LessonPublicSummaryDto;
import com.example.demo.lesson.authoring.LessonService;
import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.quiz.QuizQueryService;
import com.example.demo.quiz.dto.QuizResponseDto;

@RestController
@RequestMapping("/api/lessons")
@Tag(name = "Lessons", description = "Lesson browsing and contributor lesson management endpoints")
public class LessonController {

    private final LessonService lessonService;
    private final QuizQueryService quizQueryService;

    public LessonController(LessonService lessonService, QuizQueryService quizQueryService) {
        this.lessonService = lessonService;
        this.quizQueryService = quizQueryService;
    }

    @GetMapping
    @Operation(summary = "List public lessons", description = "Returns approved, non-deleted lessons. Optional filter by concept public IDs.")
    public List<LessonPublicSummaryDto> getAllLessons(
            @RequestParam(required = false) List<UUID> conceptPublicIds
    ) {
        return lessonService.findPublicLessons(conceptPublicIds);
    }

    @GetMapping("/{lessonPublicId}")
    @Operation(summary = "Get lesson detail", description = "Owner gets full contributor view; others get public-approved lesson view")
    public LessonDetailView getLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.getLessonDetailForUser(lessonPublicId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create lesson", description = "Creates a lesson in UNPUBLISHED, PENDING, or REJECTED status depending on submit flag and moderation outcome")
    public LessonDetailDto createLesson(
            @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        if (Boolean.TRUE.equals(request.submit())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please add at least one quiz before publishing.");
        }
        return lessonService.createLesson(request, user);
    }

    @PutMapping("/{lessonPublicId}")
    @Operation(summary = "Update lesson content", description = "Contributor owner updates title and content")
    public LessonDetailDto updateLesson(
            @PathVariable UUID lessonPublicId,
            @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.updateLesson(lessonPublicId, request, user);
    }

    @PatchMapping("/{lessonPublicId}")
    @Operation(
            summary = "Apply lesson publication action",
            description = "Applies SUBMIT or UNPUBLISH publication action for contributor-owned lessons."
    )
    public LessonDetailDto applyPublicationAction(
            @PathVariable UUID lessonPublicId,
            @RequestBody LessonPublicationActionRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase();
        if ("SUBMIT".equals(action)) {
            List<QuizResponseDto> quizzes = quizQueryService.getQuizzesForLesson(lessonPublicId, user);
            if (quizzes.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please add at least one quiz before publishing.");
            }
            return lessonService.submitLesson(lessonPublicId, user);
        }
        if ("UNPUBLISH".equals(action)) {
            return lessonService.unpublishLesson(lessonPublicId, user);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported action");
    }

    @DeleteMapping("/{lessonPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete lesson", description = "Owner-only soft delete regardless of moderation status")
    public void softDeleteLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        lessonService.softDeleteLesson(lessonPublicId, user);
    }

}
