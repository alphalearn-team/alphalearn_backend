package com.example.demo.lessonenrollment;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@ExtendWith(MockitoExtension.class)
class LessonEnrollmentControllerTest {

    @Mock
    private LessonEnrollmentService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LessonEnrollmentController(service))
                .build();
    }

    @Test
    void getEnrollmentsReturnsEnrollmentList() throws Exception {
        UUID learnerId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(service.getAllEnrollments()).thenReturn(List.of(
                new LessonEnrollmentPublicDTO(
                        7,
                        learnerId,
                        lessonId,
                        "APPROVED",
                        true,
                        OffsetDateTime.parse("2026-03-10T09:30:00Z")
                )
        ));

        mockMvc.perform(get("/api/lessonenrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(7))
                .andExpect(jsonPath("$[0].learnerPublicId").value(learnerId.toString()))
                .andExpect(jsonPath("$[0].lessonPublicId").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].moderationStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].completed").value(true))
                .andExpect(jsonPath("$[0].firstCompletedAt").value("2026-03-10T09:30:00Z"));
    }
}
