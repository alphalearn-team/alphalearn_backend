package com.example.demo.friendship.request;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.friendship.request.dto.FriendRequestDTO;
import com.example.demo.learner.Learner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class FriendRequestControllerTest {

    @Mock
    private FriendRequestService friendRequestService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new FriendRequestController(friendRequestService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void postFriendRequestUsesBodyReceiverPublicId() throws Exception {
        SupabaseAuthUser authUser = learnerUser();
        Learner learner = authUser.learner();
        UUID receiverPublicId = UUID.randomUUID();
        FriendRequestDTO dto = new FriendRequestDTO(
                1L,
                receiverPublicId,
                "receiver",
                FriendRequestStatus.PENDING,
                OffsetDateTime.parse("2026-03-01T00:00:00Z")
        );

        when(friendRequestService.sendRequest(eq(learner), eq(receiverPublicId))).thenReturn(dto);

        mockMvc.perform(post("/api/friend-requests")
                        .principal(authToken(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPublicId\":\"" + receiverPublicId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1))
                .andExpect(jsonPath("$.otherUserPublicId").value(receiverPublicId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getFriendRequestsAcceptsLowercaseDirection() throws Exception {
        SupabaseAuthUser authUser = learnerUser();
        Learner learner = authUser.learner();
        UUID otherUserPublicId = UUID.randomUUID();

        when(friendRequestService.getPendingRequests(eq(learner))).thenReturn(List.of(
                new FriendRequestDTO(
                        2L,
                        otherUserPublicId,
                        "incoming-user",
                        FriendRequestStatus.PENDING,
                        OffsetDateTime.parse("2026-03-01T00:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/friend-requests?direction=incoming")
                        .principal(authToken(authUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(2))
                .andExpect(jsonPath("$[0].otherUsername").value("incoming-user"));
    }

    @Test
    void patchFriendRequestStatusReturnsNoContent() throws Exception {
        SupabaseAuthUser authUser = learnerUser();

        mockMvc.perform(patch("/api/friend-requests/{requestId}", 5L)
                        .principal(authToken(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFriendRequestsRejectsInvalidDirection() throws Exception {
        SupabaseAuthUser authUser = learnerUser();

        mockMvc.perform(get("/api/friend-requests?direction=sideways")
                        .principal(authToken(authUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchFriendRequestRejectsPendingStatus() throws Exception {
        SupabaseAuthUser authUser = learnerUser();
        Learner learner = authUser.learner();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Only APPROVED or REJECTED are supported"))
                .when(friendRequestService)
                .updateRequestStatus(eq(learner), eq(9L), eq(FriendRequestStatus.PENDING));

        mockMvc.perform(patch("/api/friend-requests/{requestId}", 9L)
                        .principal(authToken(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allEndpointsRejectWhenLearnerMissing() throws Exception {
        SupabaseAuthUser userWithoutLearner = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        mockMvc.perform(post("/api/friend-requests")
                        .principal(authToken(userWithoutLearner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPublicId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/friend-requests?direction=incoming")
                        .principal(authToken(userWithoutLearner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/friend-requests/{requestId}", 1L)
                        .principal(authToken(userWithoutLearner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/friend-requests/{requestId}", 1L)
                        .principal(authToken(userWithoutLearner)))
                .andExpect(status().isForbidden());
    }

    private SupabaseAuthenticationToken authToken(SupabaseAuthUser user) {
        return new SupabaseAuthenticationToken(
                user,
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject(user.userId().toString())
                        .build(),
                List.of()
        );
    }

    private SupabaseAuthUser learnerUser() {
        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(
                learnerId,
                UUID.randomUUID(),
                "learner",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                (short) 0
        );
        return new SupabaseAuthUser(learnerId, learner, null);
    }
}
