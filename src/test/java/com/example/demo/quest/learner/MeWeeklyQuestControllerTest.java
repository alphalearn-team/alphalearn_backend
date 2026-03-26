package com.example.demo.quest.learner;

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
import com.example.demo.quest.weekly.enums.QuestSubmissionMode;
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
        @Mock
        private LearnerQuestChallengeFeedQueryService learnerQuestChallengeFeedQueryService;
        @Mock
        private QuestHistoryQueryService questHistoryQueryService;

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
                learnerQuestChallengeSubmissionService,
                learnerQuestChallengeFeedQueryService,
                questHistoryQueryService
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
                        List.of(new QuestChallengeTaggedFriendDto(UUID.randomUUID(), "friend-one")),
                        OffsetDateTime.parse("2026-03-22T01:00:00+08:00"),
                        OffsetDateTime.parse("2026-03-22T01:05:00+08:00")
                )
        );
        when(learnerWeeklyQuestQueryService.getCurrentWeeklyQuest(any())).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/me/weekly-quests").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStartAt").value("2026-03-22T00:00:00+08:00"))
                .andExpect(jsonPath("$.concept.title").value("fire"))
                .andExpect(jsonPath("$.quest.title").value("Video + Caption"))
                .andExpect(jsonPath("$.questChallengeSubmission.originalFilename").value("evidence.mp4"))
                .andExpect(jsonPath("$.questChallengeSubmission.taggedFriends[0].learnerUsername").value("friend-one"))
                .andExpect(jsonPath("$.editable").doesNotExist())
                .andExpect(jsonPath("$.activationSource").doesNotExist())
                .andExpect(jsonPath("$.createdByAdminId").doesNotExist());
    }

    @Test
    void returnsEmptyBodyWhenNoCurrentWeeklyQuestExists() throws Exception {
        when(learnerWeeklyQuestQueryService.getCurrentWeeklyQuest(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/me/weekly-quests").accept(MediaType.APPLICATION_JSON))
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

        mockMvc.perform(post("/api/me/weekly-quests/quest-challenge-uploads")
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
                List.of(new QuestChallengeTaggedFriendDto(UUID.randomUUID(), "friend-one")),
                OffsetDateTime.parse("2026-03-14T10:00:00Z"),
                OffsetDateTime.parse("2026-03-14T10:05:00Z")
        );

        when(learnerQuestChallengeSubmissionService.getCurrentSubmission(any())).thenReturn(Optional.of(view));

        mockMvc.perform(get("/api/me/weekly-quests/quest-challenge-submissions")
                        .queryParam("scope", "CURRENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("quest-challenges/assignment/learner/object-evidence.mp4"))
                .andExpect(jsonPath("$.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.caption").value("Learner caption"))
                .andExpect(jsonPath("$.taggedFriends[0].learnerUsername").value("friend-one"));
    }

    @Test
    void savesQuestChallengeSubmission() throws Exception {
        QuestChallengeSubmissionRequest request = new QuestChallengeSubmissionRequest(
                "quest-challenges/assignment/learner/object-evidence.mp4",
                "evidence.mp4",
                "Learner caption",
                List.of(UUID.randomUUID())
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
                List.of(new QuestChallengeTaggedFriendDto(request.taggedFriendPublicIds().get(0), "friend-one")),
                OffsetDateTime.parse("2026-03-14T10:00:00Z"),
                OffsetDateTime.parse("2026-03-14T10:05:00Z")
        );

        when(learnerQuestChallengeSubmissionService.saveCurrentSubmission(eq(request.toCommand()), any())).thenReturn(view);

        mockMvc.perform(put("/api/me/weekly-quests/quest-challenge-submissions")
                        .queryParam("scope", "CURRENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("evidence.mp4"))
                .andExpect(jsonPath("$.caption").value("Learner caption"))
                .andExpect(jsonPath("$.taggedFriends[0].learnerPublicId").value(request.taggedFriendPublicIds().get(0).toString()))
                .andExpect(jsonPath("$.fileSizeBytes").value(1024));
    }

    @Test
    void returnsFriendsQuestChallengeFeed() throws Exception {
        FriendQuestChallengeFeedDto response = new FriendQuestChallengeFeedDto(
                List.of(new FriendQuestChallengeFeedItemDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "friend-one",
                        UUID.randomUUID(),
                        "Algebra",
                        UUID.randomUUID(),
                        "https://example.com/media.mp4",
                        "video/mp4",
                        "media.mp4",
                        "Great challenge this week",
                        OffsetDateTime.parse("2026-03-24T09:00:00Z"),
                        List.of(
                                new QuestChallengeTaggedFriendDto(UUID.randomUUID(), "tagged-friend-one")
                        )
                )),
                0,
                20,
                false
        );

        when(learnerQuestChallengeFeedQueryService.getFriendsFeed(
                any(),
                eq(0),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(response);

        mockMvc.perform(get("/api/me/weekly-quests/entries")
                        .queryParam("view", "FEED")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.items[0].learnerUsername").value("friend-one"))
                .andExpect(jsonPath("$.items[0].conceptTitle").value("Algebra"))
                .andExpect(jsonPath("$.items[0].taggedFriends[0].learnerUsername").value("tagged-friend-one"));
    }

    @Test
    void returnsFriendsQuestChallengeFeedWithSubmittedAtRange() throws Exception {
        FriendQuestChallengeFeedDto response = new FriendQuestChallengeFeedDto(List.of(), 0, 20, false);
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-03-14T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-17T23:59:59Z");

        when(learnerQuestChallengeFeedQueryService.getFriendsFeed(
                any(),
                eq(0),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                eq(submittedFrom),
                eq(submittedTo)
        )).thenReturn(response);

        mockMvc.perform(get("/api/me/weekly-quest/friends/feed")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("submittedFrom", "2026-03-14T00:00:00Z")
                        .queryParam("submittedTo", "2026-03-17T23:59:59Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void returnsBadRequestWhenFeedPageIsInvalid() throws Exception {
        when(learnerQuestChallengeFeedQueryService.getFriendsFeed(
                any(),
                eq(-1),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        ))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "page must be greater than or equal to 0"
                ));

        mockMvc.perform(get("/api/me/weekly-quests/entries")
                        .queryParam("view", "FEED")
                        .queryParam("page", "-1")
                        .queryParam("size", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsMyQuestHistory() throws Exception {
        QuestHistoryDto response = new QuestHistoryDto(
                List.of(new QuestHistoryItemDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "learner",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OffsetDateTime.parse("2026-03-23T00:00:00Z"),
                        UUID.randomUUID(),
                        "Algebra",
                        "https://example.com/media.mp4",
                        "video/mp4",
                        "media.mp4",
                        "caption",
                        OffsetDateTime.parse("2026-03-24T09:00:00Z"),
                        com.example.demo.quest.weekly.enums.SubmissionVisibility.PUBLIC
                )),
                0,
                20,
                false
        );

        when(questHistoryQueryService.getMyHistory(
                any(),
                eq(0),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        ))
                .thenReturn(response);

        mockMvc.perform(get("/api/me/weekly-quests/records")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].learnerUsername").value("learner"))
                .andExpect(jsonPath("$.items[0].visibility").value("PUBLIC"));
    }

    @Test
    void returnsMyQuestHistoryWithSubmittedAtRange() throws Exception {
        QuestHistoryDto response = new QuestHistoryDto(List.of(), 0, 20, false);
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-31T23:59:59Z");

        when(questHistoryQueryService.getMyHistory(
                any(),
                eq(0),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                eq(submittedFrom),
                eq(submittedTo)
        )).thenReturn(response);

        mockMvc.perform(get("/api/me/weekly-quest/history")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("submittedFrom", "2026-03-01T00:00:00Z")
                        .queryParam("submittedTo", "2026-03-31T23:59:59Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void returnsFriendQuestHistory() throws Exception {
        UUID friendPublicId = UUID.randomUUID();
        QuestHistoryDto response = new QuestHistoryDto(List.of(), 0, 20, false);

        when(questHistoryQueryService.getFriendHistory(
                any(),
                eq(friendPublicId),
                eq(0),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        ))
                .thenReturn(response);

        mockMvc.perform(get("/api/me/weekly-quests/records")
                        .queryParam("friendPublicId", friendPublicId.toString())
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void returnsFriendQuestHistoryWithSubmittedAtRange() throws Exception {
        UUID friendPublicId = UUID.randomUUID();
        QuestHistoryDto response = new QuestHistoryDto(List.of(), 0, 20, false);
        OffsetDateTime submittedFrom = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        OffsetDateTime submittedTo = OffsetDateTime.parse("2026-03-31T23:59:59Z");

        when(questHistoryQueryService.getFriendHistory(
                any(),
                eq(friendPublicId),
                eq(0),
                eq(20),
                org.mockito.ArgumentMatchers.isNull(),
                eq(submittedFrom),
                eq(submittedTo)
        )).thenReturn(response);

        mockMvc.perform(get("/api/me/weekly-quest/friends/{friendPublicId}/history", friendPublicId)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("submittedFrom", "2026-03-01T00:00:00Z")
                        .queryParam("submittedTo", "2026-03-31T23:59:59Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
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
