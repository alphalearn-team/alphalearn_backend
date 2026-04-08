package com.example.demo.lesson.read;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.authoring.LessonService;
import com.example.demo.lesson.dto.LessonContributorSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/lessons")
@Tag(name = "My Lessons", description = "Authenticated contributor lesson listing endpoint")
public class MeLessonController {

    private final LessonService lessonService;

    public MeLessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    @Operation(summary = "List my authored lessons", description = "Returns non-deleted lessons authored by the authenticated contributor")
    public List<LessonContributorSummaryDto> getMyLessons(
            @AuthenticationPrincipal SupabaseAuthUser user,
            @RequestParam(required = false) List<UUID> conceptPublicIds
    ) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        return lessonService.getMyAuthoredLessons(user.userId(), conceptPublicIds);
    }
}
