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

import com.example.demo.lesson.dto.CreateLessonRequest;
import com.example.demo.lesson.dto.UpdateLessonRequest;
import com.example.demo.lesson.dto.LessonContributorSummaryDto;
import com.example.demo.lesson.dto.LessonDetailDto;
import com.example.demo.lesson.dto.LessonDetailView;
import com.example.demo.lesson.dto.LessonPublicSummaryDto;
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
            @RequestParam(required = false) List<UUID> conceptPublicIds,
            @RequestParam(defaultValue = "any") String conceptsMatch
    ) {
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return lessonService.findPublicLessons(conceptPublicIds, matchMode);
    }

    @GetMapping("/mine")
    @Operation(summary = "List my lessons", description = "Authenticated owner-only; optional concept filter")
    public List<LessonContributorSummaryDto> getMyLessons(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) List<UUID> conceptPublicIds,
            @RequestParam(defaultValue = "any") String conceptsMatch
    ) {
        if (user == null || user.userId() == null || !user.isContributor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contributor access required");
        }
        UUID contributorId = user.userId();
        ConceptsMatchMode matchMode = ConceptsMatchMode.fromRequest(conceptsMatch);
        return lessonService.getLessonsByContributor(contributorId, conceptPublicIds, matchMode);
    }

    @GetMapping("/{lessonPublicId}")
    @Operation(summary = "Get lesson by ID")
    public LessonDetailView getLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.getLessonDetailForUser(lessonPublicId, user);
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

    @PutMapping("/{lessonPublicId}")
    @Operation(summary = "Update lesson content")
    public LessonDetailDto updateLesson(
            @PathVariable UUID lessonPublicId,
            @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.updateLesson(lessonPublicId, request, user);
    }

    @PostMapping("/{lessonPublicId}/submit")
    @Operation(summary = "Submit lesson for review")
    public LessonDetailDto submitLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.submitLesson(lessonPublicId, user);
    }

    @PostMapping("/{lessonPublicId}/unpublish")
    @Operation(summary = "Unpublish lesson")
    public LessonDetailDto unpublishLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return lessonService.unpublishLesson(lessonPublicId, user);
    }

    @DeleteMapping("/{lessonPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete lesson", description = "Owner-only; lesson must be unpublished")
    public void softDeleteLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        lessonService.softDeleteLesson(lessonPublicId, user);
    }

}
