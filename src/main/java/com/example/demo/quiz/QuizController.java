package com.example.demo.quiz;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.quiz.dto.CreateQuizRequest;
import com.example.demo.quiz.dto.QuizResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes", description = "Endpoints for creating and managing lesson quizzes")
public class QuizController {

    private final QuizService quizService;
    private final QuizQueryService quizQueryService;

    public QuizController(QuizService quizService, QuizQueryService quizQueryService) {
        this.quizService = quizService;
        this.quizQueryService = quizQueryService;
    }

    @GetMapping("/{lessonPublicId}")
    @Operation(summary = "List quizzes for lesson", description = "Returns learner-safe quizzes for the specified lesson.")
    public List<QuizResponseDto> getQuizzesForLesson(
            @PathVariable UUID lessonPublicId,
            @AuthenticationPrincipal SupabaseAuthUser user) {
        return quizQueryService.getQuizzesForLesson(lessonPublicId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create quiz", description = "Creates a new quiz linked to a specific lesson.")
    public void createQuiz(@Valid @RequestBody CreateQuizRequest request) {
        quizService.createQuiz(request);
    }
}
