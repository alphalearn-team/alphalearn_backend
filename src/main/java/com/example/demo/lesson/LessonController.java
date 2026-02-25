package com.example.demo.lesson;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.lesson.dto.request.CreateLessonRequest;
import com.example.demo.lesson.dto.request.UpdateLessonRequest;
import com.example.demo.lesson.dto.response.LessonContributorSummaryDto;
import com.example.demo.lesson.dto.response.LessonDetailDto;
import com.example.demo.lesson.dto.response.LessonDetailView;
import com.example.demo.lesson.dto.response.LessonPublicSummaryDto;
import com.example.demo.lesson.query.ConceptsMatchMode;
import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/lessons")
@Tag(name = "Lessons", description = "Lesson creation and moderation endpoints")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    @Operation(summary = "List lessons", description = "Optionally filter by conceptId")
    public List<LessonPublicSummaryDto> getAllLessons(
            @RequestParam(required = false) List<Integer> conceptIds,
            @RequestParam(defaultValue = "any") String conceptsMatch
    ) {
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return lessonService.findPublicLessons(conceptIds, matchMode);
    }

    @GetMapping("/mine")
    @Operation(summary = "List my lessons", description = "Authenticated owner-only; optional concept filter")
    public List<LessonContributorSummaryDto> getMyLessons(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) List<Integer> conceptIds,
            @RequestParam(defaultValue = "any") String conceptsMatch
    ) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        UUID contributorId = user.userId();
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return lessonService.getLessonsByContributor(contributorId, conceptIds, matchMode);
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson by ID")
    public LessonDetailView getLesson(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.getLessonDetailForUser(lessonId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create lesson")
    public LessonDetailDto createLesson(
            @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.createLesson(request, user);
    }

    @PutMapping("/{lessonId}")
    @Operation(summary = "Update lesson content")
    public LessonDetailDto updateLesson(
            @PathVariable Integer lessonId,
            @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.updateLesson(lessonId, request, user);
    }

    @PostMapping("/{lessonId}/submit")
    @Operation(summary = "Submit lesson for review")
    public LessonDetailDto submitLesson(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.submitLesson(lessonId, user);
    }

    @PostMapping("/{lessonId}/unpublish")
    @Operation(summary = "Unpublish lesson")
    public LessonDetailDto unpublishLesson(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.unpublishLesson(lessonId, user);
    }

    @DeleteMapping("/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete lesson", description = "Owner-only; lesson must be unpublished")
    public void softDeleteLesson(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        lessonService.softDeleteLesson(lessonId, user);
    }

}
