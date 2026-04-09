package com.example.demo.game.lobby.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.LearnerGameLobbyService;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.invite.dto.GameLobbyInviteDto;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.notification.NotificationService;
import com.example.demo.notification.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GameLobbyInviteServiceTest {

    @Mock
    private GameLobbyInviteRepository inviteRepository;

    @Mock
    private GameLobbyRepository lobbyRepository;

    @Mock
    private GameLobbyMemberRepository lobbyMemberRepository;

    @Mock
    private LearnerRepository learnerRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private LearnerGameLobbyService learnerGameLobbyService;

    @Mock
    private NotificationService notificationService;

    private GameLobbyInviteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T00:00:00Z"), ZoneOffset.UTC);
        service = new GameLobbyInviteService(
                inviteRepository,
                lobbyRepository,
                lobbyMemberRepository,
                learnerRepository,
                friendRepository,
                learnerGameLobbyService,
                notificationService,
                clock
        );
    }

    @Test
    void inviteFriendsCreatesPendingInviteAndNotification() {
        Learner host = learner(UUID.randomUUID(), UUID.randomUUID(), "host");
        Learner friend = learner(UUID.randomUUID(), UUID.randomUUID(), "friend");
        SupabaseAuthUser authUser = authUser(host);
        GameLobby lobby = lobby(host.getId());

        when(lobbyRepository.findByPublicIdForUpdate(lobby.getPublicId())).thenReturn(Optional.of(lobby));
        when(learnerRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(learnerRepository.findAllByPublicIdIn(any())).thenReturn(List.of(friend));
        when(friendRepository.existsFriendship(host.getId(), friend.getId())).thenReturn(true);
        when(lobbyMemberRepository.existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), friend.getId())).thenReturn(false);
        when(inviteRepository.findByLobby_IdAndReceiverLearnerIdAndStatus(lobby.getId(), friend.getId(), GameLobbyInviteStatus.PENDING))
                .thenReturn(Optional.empty());
        when(inviteRepository.save(any(GameLobbyInvite.class))).thenAnswer(invocation -> {
            GameLobbyInvite invite = invocation.getArgument(0);
            if (invite.getPublicId() == null) {
                ReflectionTestUtils.setField(invite, "publicId", UUID.randomUUID());
            }
            return invite;
        });

        List<GameLobbyInviteDto> result = service.inviteFriends(authUser, lobby.getPublicId(), List.of(friend.getPublicId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(GameLobbyInviteStatus.PENDING);
        verify(notificationService).create(
                eq(friend.getId()),
                eq("host invited you to join game lobby ABCD2345"),
                eq(NotificationType.GAME_LOBBY_INVITE),
                anyMap()
        );
    }

    @Test
    void inviteFriendsRejectsWhenCallerIsNotHost() {
        Learner host = learner(UUID.randomUUID(), UUID.randomUUID(), "host");
        Learner caller = learner(UUID.randomUUID(), UUID.randomUUID(), "caller");
        GameLobby lobby = lobby(host.getId());

        when(lobbyRepository.findByPublicIdForUpdate(lobby.getPublicId())).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> service.inviteFriends(authUser(caller), lobby.getPublicId(), List.of(UUID.randomUUID())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only lobby host can invite friends");
    }

    @Test
    void inviteFriendsRejectsNonFriend() {
        Learner host = learner(UUID.randomUUID(), UUID.randomUUID(), "host");
        Learner friend = learner(UUID.randomUUID(), UUID.randomUUID(), "friend");
        SupabaseAuthUser authUser = authUser(host);
        GameLobby lobby = lobby(host.getId());

        when(lobbyRepository.findByPublicIdForUpdate(lobby.getPublicId())).thenReturn(Optional.of(lobby));
        when(learnerRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(learnerRepository.findAllByPublicIdIn(any())).thenReturn(List.of(friend));
        when(friendRepository.existsFriendship(host.getId(), friend.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.inviteFriends(authUser, lobby.getPublicId(), List.of(friend.getPublicId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Can only invite existing friends");
    }

    @Test
    void inviteFriendsReturnsExistingPendingInviteWithoutSendingNewNotification() {
        Learner host = learner(UUID.randomUUID(), UUID.randomUUID(), "host");
        Learner friend = learner(UUID.randomUUID(), UUID.randomUUID(), "friend");
        SupabaseAuthUser authUser = authUser(host);
        GameLobby lobby = lobby(host.getId());

        GameLobbyInvite existing = new GameLobbyInvite();
        existing.setLobby(lobby);
        existing.setSenderLearnerId(host.getId());
        existing.setReceiverLearnerId(friend.getId());
        existing.setStatus(GameLobbyInviteStatus.PENDING);
        existing.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        ReflectionTestUtils.setField(existing, "publicId", UUID.randomUUID());

        when(lobbyRepository.findByPublicIdForUpdate(lobby.getPublicId())).thenReturn(Optional.of(lobby));
        when(learnerRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(learnerRepository.findAllByPublicIdIn(any())).thenReturn(List.of(friend));
        when(friendRepository.existsFriendship(host.getId(), friend.getId())).thenReturn(true);
        when(lobbyMemberRepository.existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), friend.getId())).thenReturn(false);
        when(inviteRepository.findByLobby_IdAndReceiverLearnerIdAndStatus(lobby.getId(), friend.getId(), GameLobbyInviteStatus.PENDING))
                .thenReturn(Optional.of(existing));

        List<GameLobbyInviteDto> result = service.inviteFriends(authUser, lobby.getPublicId(), List.of(friend.getPublicId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).invitePublicId()).isEqualTo(existing.getPublicId());
        assertThat(existing.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        verify(notificationService, never()).create(eq(friend.getId()), any(), eq(NotificationType.GAME_LOBBY_INVITE), anyMap());
    }

    @Test
    void respondAcceptJoinsLobbyAndMarksInviteAccepted() {
        Learner sender = learner(UUID.randomUUID(), UUID.randomUUID(), "sender");
        Learner receiver = learner(UUID.randomUUID(), UUID.randomUUID(), "receiver");
        SupabaseAuthUser receiverAuth = authUser(receiver);
        GameLobby lobby = lobby(sender.getId());

        GameLobbyInvite invite = new GameLobbyInvite();
        invite.setLobby(lobby);
        invite.setSenderLearnerId(sender.getId());
        invite.setReceiverLearnerId(receiver.getId());
        invite.setStatus(GameLobbyInviteStatus.PENDING);
        invite.setCreatedAt(OffsetDateTime.parse("2026-04-08T00:00:00Z"));
        ReflectionTestUtils.setField(invite, "publicId", UUID.randomUUID());

        when(inviteRepository.findByPublicIdForUpdate(invite.getPublicId())).thenReturn(Optional.of(invite));
        when(lobbyRepository.findById(lobby.getId())).thenReturn(Optional.of(lobby));
        when(learnerGameLobbyService.joinPrivateLobby(eq(receiverAuth), any(JoinPrivateGameLobbyRequest.class)))
                .thenReturn(new JoinedPrivateGameLobbyDto(
                        lobby.getPublicId(),
                        lobby.getLobbyCode(),
                        true,
                        lobby.getConceptPoolMode(),
                        lobby.getPinnedYearMonth(),
                        lobby.getCreatedAt(),
                        OffsetDateTime.parse("2026-04-09T00:00:00Z"),
                        false
                ));
        when(inviteRepository.save(invite)).thenReturn(invite);
        when(learnerRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(learnerRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

        GameLobbyInviteDto response = service.respondToInvite(receiverAuth, invite.getPublicId(), "ACCEPT");

        assertThat(response.status()).isEqualTo(GameLobbyInviteStatus.ACCEPTED);
        verify(notificationService).create(
                eq(sender.getId()),
                eq("receiver accepted your game invite."),
                eq(NotificationType.GAME_LOBBY_INVITE_ACCEPTED),
                anyMap()
        );
    }

    @Test
    void respondRejectMarksInviteRejected() {
        Learner sender = learner(UUID.randomUUID(), UUID.randomUUID(), "sender");
        Learner receiver = learner(UUID.randomUUID(), UUID.randomUUID(), "receiver");
        SupabaseAuthUser receiverAuth = authUser(receiver);
        GameLobby lobby = lobby(sender.getId());

        GameLobbyInvite invite = new GameLobbyInvite();
        invite.setLobby(lobby);
        invite.setSenderLearnerId(sender.getId());
        invite.setReceiverLearnerId(receiver.getId());
        invite.setStatus(GameLobbyInviteStatus.PENDING);
        invite.setCreatedAt(OffsetDateTime.parse("2026-04-08T00:00:00Z"));
        ReflectionTestUtils.setField(invite, "publicId", UUID.randomUUID());

        when(inviteRepository.findByPublicIdForUpdate(invite.getPublicId())).thenReturn(Optional.of(invite));
        when(inviteRepository.save(invite)).thenReturn(invite);
        when(learnerRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(learnerRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

        GameLobbyInviteDto response = service.respondToInvite(receiverAuth, invite.getPublicId(), "REJECT");

        assertThat(response.status()).isEqualTo(GameLobbyInviteStatus.REJECTED);
        verify(notificationService).create(
                eq(sender.getId()),
                eq("receiver rejected your game invite."),
                eq(NotificationType.GAME_LOBBY_INVITE_REJECTED),
                anyMap()
        );
    }

    @Test
    void respondAcceptExpiresWhenLobbyMissing() {
        Learner sender = learner(UUID.randomUUID(), UUID.randomUUID(), "sender");
        Learner receiver = learner(UUID.randomUUID(), UUID.randomUUID(), "receiver");
        SupabaseAuthUser receiverAuth = authUser(receiver);
        GameLobby lobby = lobby(sender.getId());

        GameLobbyInvite invite = new GameLobbyInvite();
        invite.setLobby(lobby);
        invite.setSenderLearnerId(sender.getId());
        invite.setReceiverLearnerId(receiver.getId());
        invite.setStatus(GameLobbyInviteStatus.PENDING);
        ReflectionTestUtils.setField(invite, "publicId", UUID.randomUUID());

        when(inviteRepository.findByPublicIdForUpdate(invite.getPublicId())).thenReturn(Optional.of(invite));
        when(lobbyRepository.findById(lobby.getId())).thenReturn(Optional.empty());
        when(inviteRepository.save(invite)).thenReturn(invite);

        assertThatThrownBy(() -> service.respondToInvite(receiverAuth, invite.getPublicId(), "ACCEPT"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Lobby is no longer joinable");

        assertThat(invite.getStatus()).isEqualTo(GameLobbyInviteStatus.EXPIRED);
    }

    @Test
    void cancelInviteBySenderMarksCanceled() {
        Learner sender = learner(UUID.randomUUID(), UUID.randomUUID(), "sender");
        Learner receiver = learner(UUID.randomUUID(), UUID.randomUUID(), "receiver");
        GameLobbyInvite invite = new GameLobbyInvite();
        invite.setSenderLearnerId(sender.getId());
        invite.setReceiverLearnerId(receiver.getId());
        invite.setStatus(GameLobbyInviteStatus.PENDING);
        ReflectionTestUtils.setField(invite, "publicId", UUID.randomUUID());

        when(inviteRepository.findByPublicIdForUpdate(invite.getPublicId())).thenReturn(Optional.of(invite));

        service.cancelInvite(authUser(sender), invite.getPublicId());

        ArgumentCaptor<GameLobbyInvite> captor = ArgumentCaptor.forClass(GameLobbyInvite.class);
        verify(inviteRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GameLobbyInviteStatus.CANCELED);
    }

    private SupabaseAuthUser authUser(Learner learner) {
        return new SupabaseAuthUser(learner.getId(), learner, null, "test@example.com");
    }

    private Learner learner(UUID internalId, UUID publicId, String username) {
        Learner learner = new Learner();
        learner.setId(internalId);
        learner.setPublicId(publicId);
        learner.setUsername(username);
        learner.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return learner;
    }

    private GameLobby lobby(UUID hostLearnerId) {
        GameLobby lobby = new GameLobby();
        ReflectionTestUtils.setField(lobby, "id", 99L);
        ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
        lobby.setLobbyCode("ABCD2345");
        lobby.setPrivateLobby(true);
        lobby.setHostLearnerId(hostLearnerId);
        lobby.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return lobby;
    }
}
