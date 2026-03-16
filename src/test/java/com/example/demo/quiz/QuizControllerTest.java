package com.example.demo.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.quiz.dto.QuizOptionDto;
import com.example.demo.quiz.dto.QuizQuestionResponseDto;
import com.example.demo.quiz.dto.QuizResponseDto;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock
    private QuizService quizService;

    @Mock
    private QuizQueryService quizQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new QuizController(quizService, quizQueryService))
                .build();
    }

    @Test
    void getQuizzesForLessonReturnsLearnerSafeQuizList() throws Exception {
        UUID lessonPublicId = UUID.randomUUID();
        UUID quizPublicId = UUID.randomUUID();
        UUID questionPublicId = UUID.randomUUID();

        when(quizQueryService.getQuizzesForLesson(lessonPublicId)).thenReturn(List.of(
                new QuizResponseDto(
                        quizPublicId,
                        lessonPublicId,
                        OffsetDateTime.parse("2026-03-16T10:15:30Z"),
                        List.of(
                                new QuizQuestionResponseDto(
                                        questionPublicId,
                                        "single-choice",
                                        "What is 2 + 2?",
                                        0,
                                        List.of(
                                                new QuizOptionDto("1", "3"),
                                                new QuizOptionDto("2", "4")
                                        )
                                )
                        )
                )
        ));

        mockMvc.perform(get("/api/quizzes/{lessonPublicId}", lessonPublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quizPublicId").value(quizPublicId.toString()))
                .andExpect(jsonPath("$[0].lessonPublicId").value(lessonPublicId.toString()))
                .andExpect(jsonPath("$[0].questions[0].questionPublicId").value(questionPublicId.toString()))
                .andExpect(jsonPath("$[0].questions[0].type").value("single-choice"))
                .andExpect(jsonPath("$[0].questions[0].options[1].text").value("4"))
                .andExpect(jsonPath("$[0].questions[0].correctOptionId").doesNotExist())
                .andExpect(jsonPath("$[0].questions[0].correctOptionIds").doesNotExist())
                .andExpect(jsonPath("$[0].questions[0].correctBoolean").doesNotExist());
    }

    @Test
    void getQuizzesForLessonReturnsEmptyArrayWhenLessonHasNoQuizzes() throws Exception {
        UUID lessonPublicId = UUID.randomUUID();

        when(quizQueryService.getQuizzesForLesson(lessonPublicId)).thenReturn(List.of());

        mockMvc.perform(get("/api/quizzes/{lessonPublicId}", lessonPublicId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getQuizzesForLessonReturnsNotFoundWhenLessonDoesNotExist() throws Exception {
        UUID lessonPublicId = UUID.randomUUID();

        when(quizQueryService.getQuizzesForLesson(lessonPublicId)).thenThrow(
                new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Lesson not found")
        );

        mockMvc.perform(get("/api/quizzes/{lessonPublicId}", lessonPublicId))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                            .isEqualTo("Lesson not found");
                });
    }
}
