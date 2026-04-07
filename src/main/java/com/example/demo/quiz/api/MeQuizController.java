package com.example.demo.quiz.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{quizPublicId}/attempts/best")
    @Operation(summary = "Get best quiz attempt", description = "Returns the authenticated learner's highest-scoring attempt summary for the quiz")
    public QuizAttemptResponse getBestQuizAttempt(
            @PathVariable UUID quizPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuizAttemptService.getBestQuizAttempt(quizPublicId, user);
    }

    @GetMapping("/{quizPublicId}/attempts/latest")
    @Operation(summary = "Get latest quiz attempt", description = "Returns the authenticated learner's most recent attempt summary for the quiz")
    public QuizAttemptResponse getLatestQuizAttempt(
            @PathVariable UUID quizPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return learnerQuizAttemptService.getLatestQuizAttempt(quizPublicId, user);
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
