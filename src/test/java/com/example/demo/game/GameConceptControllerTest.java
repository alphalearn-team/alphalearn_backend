package com.example.demo.game;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.dto.GameAssignedConceptDto;
import com.example.demo.game.dto.NextGameConceptRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class GameConceptControllerTest {

    @Mock
    private GameConceptService imposterGameConceptService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new GameConceptController(imposterGameConceptService))
                .build();
    }

    @Test
    void assignNextConceptAcceptsLobbyPublicId() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID excludedConceptId = UUID.randomUUID();
        UUID assignedConceptId = UUID.randomUUID();
        NextGameConceptRequest request = new NextGameConceptRequest(
                List.of(excludedConceptId),
                lobbyPublicId
        );

        when(imposterGameConceptService.assignNextConcept(any(), eq(request)))
                .thenReturn(new GameAssignedConceptDto(assignedConceptId, "binary tree"));

        mockMvc.perform(post("/api/games/imposter/concepts/next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conceptPublicId").value(assignedConceptId.toString()))
                .andExpect(jsonPath("$.word").value("binary tree"));
    }

    @Test
    void assignNextConceptAcceptsLobbyCode() throws Exception {
        UUID assignedConceptId = UUID.randomUUID();
        NextGameConceptRequest request = new NextGameConceptRequest(
                List.of(),
                null,
                "ABCD2345"
        );

        when(imposterGameConceptService.assignNextConcept(any(), eq(request)))
                .thenReturn(new GameAssignedConceptDto(assignedConceptId, "merge sort"));

        mockMvc.perform(post("/api/games/imposter/concepts/next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conceptPublicId").value(assignedConceptId.toString()))
                .andExpect(jsonPath("$.word").value("merge sort"));
    }
}
