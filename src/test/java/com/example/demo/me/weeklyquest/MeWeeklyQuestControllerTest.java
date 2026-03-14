package com.example.demo.me.weeklyquest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.weeklyquest.enums.QuestSubmissionMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MeWeeklyQuestControllerTest {

    @Mock
    private LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new MeWeeklyQuestController(learnerWeeklyQuestQueryService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsLearnerSafeCurrentWeeklyQuest() throws Exception {
        LearnerCurrentWeeklyQuestDto dto = new LearnerCurrentWeeklyQuestDto(
                OffsetDateTime.parse("2026-03-22T00:00:00+08:00"),
                new LearnerWeeklyQuestConceptDto(UUID.randomUUID(), "fire", "Describes something intense."),
                new LearnerWeeklyQuestDetailsDto(
                        "Video + Caption",
                        "Record a short video and write a caption using the assigned concept.",
                        QuestSubmissionMode.VIDEO_WITH_CAPTION
                )
        );
        when(learnerWeeklyQuestQueryService.getCurrentWeeklyQuest()).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/me/weekly-quest/current").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStartAt").value("2026-03-22T00:00:00+08:00"))
                .andExpect(jsonPath("$.concept.title").value("fire"))
                .andExpect(jsonPath("$.quest.title").value("Video + Caption"))
                .andExpect(jsonPath("$.editable").doesNotExist())
                .andExpect(jsonPath("$.activationSource").doesNotExist())
                .andExpect(jsonPath("$.createdByAdminId").doesNotExist());
    }

    @Test
    void returnsEmptyBodyWhenNoCurrentWeeklyQuestExists() throws Exception {
        when(learnerWeeklyQuestQueryService.getCurrentWeeklyQuest()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/me/weekly-quest/current").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
