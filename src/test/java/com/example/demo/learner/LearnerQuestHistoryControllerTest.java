package com.example.demo.learner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.quest.learner.QuestHistoryDto;
import com.example.demo.quest.learner.QuestHistoryQueryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LearnerQuestHistoryControllerTest {

    @Mock
    private QuestHistoryQueryService questHistoryQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LearnerQuestHistoryController(questHistoryQueryService))
                .build();
    }

    @Test
    void returnsLearnerPublicQuestHistory() throws Exception {
        UUID learnerPublicId = UUID.randomUUID();
        when(questHistoryQueryService.getPublicHistory(eq(learnerPublicId), eq(0), eq(20), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new QuestHistoryDto(List.of(), 0, 20, false));

        mockMvc.perform(get("/api/learners/{learnerPublicId}/weekly-quest/history", learnerPublicId)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false));
    }
}
