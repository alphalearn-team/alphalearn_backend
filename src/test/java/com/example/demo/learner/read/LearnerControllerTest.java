package com.example.demo.learner.read;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.learner.dto.LearnerPublicDto;

@ExtendWith(MockitoExtension.class)
class LearnerControllerTest {

    @Mock
    private LearnerQueryService learnerQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LearnerController(learnerQueryService)).build();
    }

    @Test
    void getLearnersReturnsPublicLearners() throws Exception {
        UUID learnerPublicId = UUID.randomUUID();
        when(learnerQueryService.getAllPublicLearners()).thenReturn(List.of(
                new LearnerPublicDto(
                        learnerPublicId,
                        "learner-user",
                        "https://cdn.example.com/learner-user.png"
                )
        ));

        mockMvc.perform(get("/api/learners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicId").value(learnerPublicId.toString()))
                .andExpect(jsonPath("$[0].username").value("learner-user"))
                .andExpect(jsonPath("$[0].profilePictureUrl").value("https://cdn.example.com/learner-user.png"));
    }
}
