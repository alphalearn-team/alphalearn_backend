package com.example.demo.contributor;

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

import com.example.demo.contributor.dto.ContributorPublicDto;

@ExtendWith(MockitoExtension.class)
class ContributorControllerTest {

    @Mock
    private ContributorQueryService contributorQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ContributorController(contributorQueryService)).build();
    }

    @Test
    void getContributorsReturnsContributorList() throws Exception {
        UUID contributorPublicId = UUID.randomUUID();
        when(contributorQueryService.getAllPublicContributors()).thenReturn(List.of(
                new ContributorPublicDto(
                        contributorPublicId,
                        "contributor-user",
                        OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                        null
                )
        ));

        mockMvc.perform(get("/api/contributors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicId").value(contributorPublicId.toString()))
                .andExpect(jsonPath("$[0].username").value("contributor-user"));
    }
}
