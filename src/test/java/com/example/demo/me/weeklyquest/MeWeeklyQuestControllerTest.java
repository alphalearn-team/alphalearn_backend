package com.example.demo.me.weeklyquest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.learner.Learner;
import com.example.demo.weeklyquest.enums.QuestSubmissionMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MeWeeklyQuestControllerTest {

    @Mock
    private LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService;
    @Mock
    private LearnerQuestChallengeUploadService learnerQuestChallengeUploadService;
    @Mock
    private LearnerQuestChallengeSubmissionService learnerQuestChallengeSubmissionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SupabaseAuthUser learnerUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new MeWeeklyQuestController(
                        learnerWeeklyQuestQueryService,
                        learnerQuestChallengeUploadService,
                        learnerQuestChallengeSubmissionService
                ))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        UUID learnerId = UUID.randomUUID();
        learnerUser = new SupabaseAuthUser(
                learnerId,
                new Learner(learnerId, UUID.randomUUID(), "learner", OffsetDateTime.parse("2026-03-01T00:00:00Z"), (short) 0),
                null
        );
        setAuthentication(learnerUser);
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
                ),
                new LearnerQuestChallengeSubmissionSummaryDto(
                        UUID.randomUUID(),
                        "quest-challenges/assignment/learner/object-evidence.mp4",
                        "https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/quest-challenges/assignment/learner/object-evidence.mp4",
                        "video/mp4",
                        "evidence.mp4",
                        1024L,
                        "Learner caption",
                        OffsetDateTime.parse("2026-03-22T01:00:00+08:00"),
                        OffsetDateTime.parse("2026-03-22T01:05:00+08:00")
                )
        );
        when(learnerWeeklyQuestQueryService.getCurrentWeeklyQuest(any())).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/me/weekly-quest/current").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStartAt").value("2026-03-22T00:00:00+08:00"))
                .andExpect(jsonPath("$.concept.title").value("fire"))
                .andExpect(jsonPath("$.quest.title").value("Video + Caption"))
                .andExpect(jsonPath("$.questChallengeSubmission.originalFilename").value("evidence.mp4"))
                .andExpect(jsonPath("$.editable").doesNotExist())
                .andExpect(jsonPath("$.activationSource").doesNotExist())
                .andExpect(jsonPath("$.createdByAdminId").doesNotExist());
    }

    @Test
    void returnsEmptyBodyWhenNoCurrentWeeklyQuestExists() throws Exception {
        when(learnerWeeklyQuestQueryService.getCurrentWeeklyQuest(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/me/weekly-quest/current").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void returnsQuestChallengeUploadInstructions() throws Exception {
        QuestChallengeUploadRequest request = new QuestChallengeUploadRequest(
                "evidence.mp4",
                "video/mp4",
                1024L
        );
        QuestChallengeUploadResponse response = new QuestChallengeUploadResponse(
                UUID.randomUUID(),
                "quest-challenges/assignment/learner/object-evidence.mp4",
                "https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/quest-challenges/assignment/learner/object-evidence.mp4",
                "https://signed-upload-url.example",
                OffsetDateTime.parse("2026-03-14T10:15:00Z"),
                Map.of("Content-Type", "video/mp4")
        );

        when(learnerQuestChallengeUploadService.createUploadInstruction(eq(request), any())).thenReturn(response);

        mockMvc.perform(post("/api/me/weekly-quest/current/quest-challenge/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("quest-challenges/assignment/learner/object-evidence.mp4"))
                .andExpect(jsonPath("$.uploadUrl").value("https://signed-upload-url.example"))
                .andExpect(jsonPath("$.requiredHeaders.Content-Type").value("video/mp4"));
    }

    @Test
    void returnsCurrentQuestChallengeSubmission() throws Exception {
        QuestChallengeSubmissionView view = new QuestChallengeSubmissionView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "quest-challenges/assignment/learner/object-evidence.mp4",
                "https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/quest-challenges/assignment/learner/object-evidence.mp4",
                "video/mp4",
                "evidence.mp4",
                1024L,
                "Learner caption",
                OffsetDateTime.parse("2026-03-14T10:00:00Z"),
                OffsetDateTime.parse("2026-03-14T10:05:00Z")
        );

        when(learnerQuestChallengeSubmissionService.getCurrentSubmission(any())).thenReturn(Optional.of(view));

        mockMvc.perform(get("/api/me/weekly-quest/current/quest-challenge/submission")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("quest-challenges/assignment/learner/object-evidence.mp4"))
                .andExpect(jsonPath("$.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.caption").value("Learner caption"));
    }

    @Test
    void savesQuestChallengeSubmission() throws Exception {
        QuestChallengeSubmissionRequest request = new QuestChallengeSubmissionRequest(
                "quest-challenges/assignment/learner/object-evidence.mp4",
                "evidence.mp4",
                "Learner caption"
        );
        QuestChallengeSubmissionView view = new QuestChallengeSubmissionView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                request.objectKey(),
                "https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/" + request.objectKey(),
                "video/mp4",
                request.originalFilename(),
                1024L,
                request.caption(),
                OffsetDateTime.parse("2026-03-14T10:00:00Z"),
                OffsetDateTime.parse("2026-03-14T10:05:00Z")
        );

        when(learnerQuestChallengeSubmissionService.saveCurrentSubmission(eq(request.toCommand()), any())).thenReturn(view);

        mockMvc.perform(put("/api/me/weekly-quest/current/quest-challenge/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("evidence.mp4"))
                .andExpect(jsonPath("$.caption").value("Learner caption"))
                .andExpect(jsonPath("$.fileSizeBytes").value(1024));
    }

    private void setAuthentication(SupabaseAuthUser user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SupabaseAuthenticationToken(
                user,
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject(user.userId().toString())
                        .build(),
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
        ));
        SecurityContextHolder.setContext(context);
    }
}
