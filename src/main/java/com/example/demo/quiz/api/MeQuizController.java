package com.example.demo.quiz.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.quiz.LearnerQuizAttemptService;
import com.example.demo.quiz.dto.QuizAttemptResponse;
import com.example.demo.quiz.dto.SubmitQuizAttemptRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/me/quizzes")
@Tag(name = "My Quizzes", description = "Learner-facing quiz attempt endpoints")
public class MeQuizController {

    private final LearnerQuizAttemptService learnerQuizAttemptService;

    public MeQuizController(LearnerQuizAttemptService learnerQuizAttemptService) {
        this.learnerQuizAttemptService = learnerQuizAttemptService;
    }

    @GetMapping("/{quizPublicId}/attempts")
    @Operation(summary = "Get quiz attempt summary", description = "Returns learner attempt summary by view: BEST or LATEST")
    public QuizAttemptResponse getQuizAttempt(
            @PathVariable UUID quizPublicId,
            @RequestParam String view,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        String normalized = view == null ? "" : view.trim().toUpperCase();
        return switch (normalized) {
            case "BEST" -> learnerQuizAttemptService.getBestQuizAttempt(quizPublicId, user);
            case "LATEST" -> learnerQuizAttemptService.getLatestQuizAttempt(quizPublicId, user);
            default -> throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "view must be BEST or LATEST"
            );
        };
    }

    @GetMapping("/{quizPublicId}/attempts/history")
    @Operation(summary = "Get quiz attempt history", description = "Returns all attempts for the learner on this quiz, newest first")
    public List<QuizAttemptResponse> getQuizAttemptHistory(
            @PathVariable UUID quizPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuizAttemptService.getQuizAttemptHistory(quizPublicId, user);
    }

    @PostMapping("/{quizPublicId}/attempts")
    @Operation(summary = "Submit quiz attempt", description = "Grades and records a learner quiz submission")
    public QuizAttemptResponse submitQuizAttempt(
            @PathVariable UUID quizPublicId,
            @RequestBody SubmitQuizAttemptRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuizAttemptService.submitQuizAttempt(quizPublicId, request, user);
    }
}
