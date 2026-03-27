package com.example.demo.friendship.friend;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.friendship.friend.dto.FriendPublicDTO;
import com.example.demo.learner.Learner;

@ExtendWith(MockitoExtension.class)
class FriendControllerTest {

    @Mock
    private FriendService friendService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FriendController(friendService)).build();
    }

    @Test
    void deleteFriendReturnsNoContent() throws Exception {
        SupabaseAuthUser authUser = learnerUser();
        UUID friendPublicId = UUID.randomUUID();

        mockMvc.perform(delete("/api/friends/{friendPublicId}", friendPublicId)
                        .principal(authToken(authUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAndDeleteRejectWhenLearnerMissing() throws Exception {
        SupabaseAuthUser userWithoutLearner = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        mockMvc.perform(get("/api/friends")
                        .principal(authToken(userWithoutLearner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/friends/{friendPublicId}", UUID.randomUUID())
                        .principal(authToken(userWithoutLearner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFriendsReturnsOkForLearner() throws Exception {
        SupabaseAuthUser authUser = learnerUser();
        when(friendService.getFriends(eq(authUser.learner()))).thenReturn(List.of(
                new FriendPublicDTO(UUID.randomUUID(), "friend-one")
        ));

        mockMvc.perform(get("/api/friends")
                        .principal(authToken(authUser)))
                .andExpect(status().isOk());
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
