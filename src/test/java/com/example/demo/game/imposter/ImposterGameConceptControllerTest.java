package com.example.demo.game.imposter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.game.imposter.dto.ImposterAssignedConceptDto;
import com.example.demo.game.imposter.dto.NextImposterConceptRequest;
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
class ImposterGameConceptControllerTest {

    @Mock
    private ImposterGameConceptService imposterGameConceptService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new ImposterGameConceptController(imposterGameConceptService))
                .build();
    }

    @Test
    void assignNextConceptAcceptsLobbyPublicId() throws Exception {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID excludedConceptId = UUID.randomUUID();
        UUID assignedConceptId = UUID.randomUUID();
        NextImposterConceptRequest request = new NextImposterConceptRequest(
                List.of(excludedConceptId),
                lobbyPublicId
        );

        when(imposterGameConceptService.assignNextConcept(any(), eq(request)))
                .thenReturn(new ImposterAssignedConceptDto(assignedConceptId, "binary tree"));

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
        NextImposterConceptRequest request = new NextImposterConceptRequest(
                List.of(),
                null,
                "ABCD2345"
        );

        when(imposterGameConceptService.assignNextConcept(any(), eq(request)))
                .thenReturn(new ImposterAssignedConceptDto(assignedConceptId, "merge sort"));

        mockMvc.perform(post("/api/games/imposter/concepts/next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conceptPublicId").value(assignedConceptId.toString()))
                .andExpect(jsonPath("$.word").value("merge sort"));
    }
}
