package com.example.demo.game.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.GameWeeklyFeaturedConceptService;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMember;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.GameLobbyCodeGenerator;
import com.example.demo.game.lobby.GameLobbyConceptPoolMode;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.GameMonthlyPackConcept;
import com.example.demo.game.lobby.invite.GameLobbyInviteRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.realtime.GameLobbyRealtimePublisher;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.game.lobby.dto.CreatePrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.LeavePrivateGameLobbyResponse;
import com.example.demo.game.lobby.dto.PrivateGameLobbyDto;
import com.example.demo.game.lobby.dto.PrivateGameLobbyLeaveResult;
import com.example.demo.game.lobby.dto.PrivateGameLobbyStateDto;
import com.example.demo.game.lobby.dto.SubmitGameVoteRequest;
import com.example.demo.game.lobby.dto.UpsertGameDrawingSnapshotRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LearnerGameLobbyServiceTest {

    @Mock
    private GameLobbyRepository imposterGameLobbyRepository;

    @Mock
    private GameLobbyMemberRepository imposterGameLobbyMemberRepository;

    @Mock
    private GameLobbyCodeGenerator imposterLobbyCodeGenerator;

    @Mock
    private GameMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    @Mock
    private GameLobbyInviteRepository gameLobbyInviteRepository;

    @Mock
    private GameWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private LearnerRepository learnerRepository;

    @Mock
    private GameLobbyRealtimePublisher imposterLobbyRealtimePublisher;

    @Mock
    private GameLobbyRealtimePresenceTracker imposterLobbyRealtimePresenceTracker;

    private LearnerGameLobbyService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC);
        service = new LearnerGameLobbyService(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                imposterLobbyCodeGenerator,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                gameLobbyInviteRepository,
                imposterWeeklyFeaturedConceptService,
                conceptRepository,
                learnerRepository,
                imposterLobbyRealtimePublisher,
                imposterLobbyRealtimePresenceTracker,
                fixedClock,
                false
        );
        lenient().when(imposterGameLobbyRepository.saveAndFlush(any(GameLobby.class))).thenAnswer(invocation -> {
            GameLobby lobby = invocation.getArgument(0);
            if (lobby.getPublicId() == null) {
                ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
            }
            return lobby;
        });
        lenient().when(imposterGameLobbyMemberRepository.saveAndFlush(any(GameLobbyMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(imposterLobbyCodeGenerator.generate()).thenReturn("ABCD2345");
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle("apple");
        lenient().when(conceptRepository.findAll()).thenReturn(List.of(concept));
        lenient().when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept(any())).thenReturn(Optional.empty());
    }

    @Test
    void createsPrivateLobbyForFullConceptPool() {
        SupabaseAuthUser user = learnerAuthUser();

        PrivateGameLobbyDto result = service.createPrivateLobby(
                user,
                new CreatePrivateGameLobbyRequest(GameLobbyConceptPoolMode.FULL_CONCEPT_POOL)
        );

        assertThat(result.publicId()).isNotNull();
        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        assertThat(result.isPrivate()).isTrue();
        assertThat(result.conceptPoolMode()).isEqualTo(GameLobbyConceptPoolMode.FULL_CONCEPT_POOL);
        assertThat(result.pinnedYearMonth()).isNull();
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        verify(imposterGameLobbyMemberRepository).saveAndFlush(any(GameLobbyMember.class));
    }

    @Test
    void createsPrivateLobbyForCurrentMonthPackWhenConfigured() {
        SupabaseAuthUser user = learnerAuthUser();
        GameMonthlyPack pack = new GameMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 91L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        GameMonthlyPackConcept conceptRow = new GameMonthlyPackConcept();
        conceptRow.setPack(pack);
        conceptRow.setSlotIndex((short) 1);
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(91L))
                .thenReturn(List.of(conceptRow));

        PrivateGameLobbyDto result = service.createPrivateLobby(
                user,
                new CreatePrivateGameLobbyRequest(GameLobbyConceptPoolMode.CURRENT_MONTH_PACK)
        );

        assertThat(result.publicId()).isNotNull();
        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        assertThat(result.conceptPoolMode()).isEqualTo(GameLobbyConceptPoolMode.CURRENT_MONTH_PACK);
        assertThat(result.pinnedYearMonth()).isEqualTo("2026-04");
    }

    @Test
    void rejectsCurrentMonthPackLobbyWhenPackMissing() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPrivateLobby(
                user,
                new CreatePrivateGameLobbyRequest(GameLobbyConceptPoolMode.CURRENT_MONTH_PACK)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current monthly imposter pack is not configured");
    }

    @Test
    void rejectsCurrentMonthPackLobbyWhenPackHasNoConcepts() {
        SupabaseAuthUser user = learnerAuthUser();
        GameMonthlyPack pack = new GameMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 91L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(91L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createPrivateLobby(
                user,
                new CreatePrivateGameLobbyRequest(GameLobbyConceptPoolMode.CURRENT_MONTH_PACK)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current monthly imposter pack has no concepts");
    }

    @Test
    void retriesLobbyCodeGenerationWhenUniqueCollisionOccurs() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterLobbyCodeGenerator.generate()).thenReturn("ABCD2345", "WXYZ6789");
        when(imposterGameLobbyRepository.saveAndFlush(any(GameLobby.class)))
                .thenThrow(new DataIntegrityViolationException("violates constraint uk_imposter_game_lobbies_lobby_code"))
                .thenAnswer(invocation -> {
                    GameLobby lobby = invocation.getArgument(0);
                    ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
                    return lobby;
                });

        PrivateGameLobbyDto result = service.createPrivateLobby(
                user,
                new CreatePrivateGameLobbyRequest(GameLobbyConceptPoolMode.FULL_CONCEPT_POOL)
        );

        assertThat(result.lobbyCode()).isEqualTo("WXYZ6789");
        verify(imposterLobbyCodeGenerator, times(2)).generate();
        verify(imposterGameLobbyRepository, times(2)).saveAndFlush(any(GameLobby.class));
    }

    @Test
    void createPrivateLobbyPropagatesWhenHostMembershipInsertFails() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterGameLobbyMemberRepository.saveAndFlush(any(GameLobbyMember.class)))
                .thenThrow(new RuntimeException("membership insert failed"));

        assertThatThrownBy(() -> service.createPrivateLobby(
                user,
                new CreatePrivateGameLobbyRequest(GameLobbyConceptPoolMode.FULL_CONCEPT_POOL)
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("membership insert failed");
    }

    @Test
    void joinPrivateLobbyCreatesMembershipForValidCode() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        when(imposterGameLobbyRepository.findByLobbyCodeForUpdate("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.empty());
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(11L, user.userId()))
                .thenReturn(Optional.empty());

        JoinedPrivateGameLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateGameLobbyRequest("ABCD2345")
        );

        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        assertThat(result.alreadyMember()).isFalse();
        assertThat(result.joinedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        verify(imposterGameLobbyMemberRepository).saveAndFlush(any(GameLobbyMember.class));
    }

    @Test
    void joinPrivateLobbyNormalizesLobbyCode() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        when(imposterGameLobbyRepository.findByLobbyCodeForUpdate("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.empty());
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(11L, user.userId()))
                .thenReturn(Optional.empty());

        JoinedPrivateGameLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateGameLobbyRequest("  abcd2345  ")
        );

        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        verify(imposterGameLobbyRepository).findByLobbyCodeForUpdate(eq("ABCD2345"));
    }

    @Test
    void joinPrivateLobbyReturnsSuccessWhenAlreadyActiveMember() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        GameLobbyMember member = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        when(imposterGameLobbyRepository.findByLobbyCodeForUpdate("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.of(member));

        JoinedPrivateGameLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateGameLobbyRequest("ABCD2345")
        );

        assertThat(result.alreadyMember()).isTrue();
        assertThat(result.joinedAt()).isEqualTo(member.getJoinedAt());
        verify(imposterGameLobbyMemberRepository, never()).saveAndFlush(any(GameLobbyMember.class));
        verify(imposterGameLobbyRepository, never()).saveAndFlush(any(GameLobby.class));
        verify(imposterLobbyRealtimePublisher, never()).publishSharedLobbyState(any(), any(), any(), any());
        verify(imposterLobbyRealtimePublisher, never()).publishViewerLobbyState(any(), any(), any(), any(), any());
    }

    @Test
    void joinPrivateLobbyAllowsRejoinAfterLearnerHasLeft() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        GameLobbyMember historical = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        historical.setLeftAt(OffsetDateTime.parse("2026-04-01T13:00:00Z"));
        historical.setRemovedByLearnerId(UUID.randomUUID());
        historical.setRemovedReason("KICKED_BY_HOST");
        when(imposterGameLobbyRepository.findByLobbyCodeForUpdate("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.empty());
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(11L, user.userId()))
                .thenReturn(Optional.of(historical));

        JoinedPrivateGameLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateGameLobbyRequest("ABCD2345")
        );

        assertThat(result.alreadyMember()).isFalse();
        assertThat(result.joinedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(historical.getLeftAt()).isNull();
        assertThat(historical.getRemovedByLearnerId()).isNull();
        assertThat(historical.getRemovedReason()).isNull();
        verify(imposterGameLobbyMemberRepository).saveAndFlush(historical);
    }

    @Test
    void joinPrivateLobbyReturnsNotFoundWhenLobbyCodeMissing() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterGameLobbyRepository.findByLobbyCodeForUpdate("ABCD2345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinPrivateLobby(
                user,
                new JoinPrivateGameLobbyRequest("ABCD2345")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Game lobby not found");
    }

    @Test
    void joinPrivateLobbyRejectsMissingLobbyCode() {
        SupabaseAuthUser user = learnerAuthUser();

        assertThatThrownBy(() -> service.joinPrivateLobby(
                user,
                new JoinPrivateGameLobbyRequest("   ")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("lobbyCode is required");
    }

    @Test
    void leavePrivateLobbyMarksActiveMemberLeftWhenLobbyNotStarted() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        GameLobbyMember self = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember other = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:30:00Z");
        Learner otherLearner = learner(other.getLearnerId(), "peer");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, user.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.of(self));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(other));
        when(learnerRepository.findAllById(List.of(other.getLearnerId()))).thenReturn(List.of(otherLearner));

        LeavePrivateGameLobbyResponse result = service.leavePrivateLobby(user, lobbyPublicId);

        assertThat(self.getLeftAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(result.result()).isEqualTo(PrivateGameLobbyLeaveResult.LEFT);
        assertThat(result.lobbyState()).isNotNull();
        assertThat(result.lobbyState().activeMemberCount()).isEqualTo(1);
        assertThat(result.lobbyState().viewerIsActiveMember()).isFalse();
        assertThat(result.lobbyState().canLeave()).isFalse();
        verify(imposterLobbyRealtimePresenceTracker).clearLobbyMembership(lobbyPublicId, user.userId());
    }

    @Test
    void leavePrivateLobbyPromotesNextHostWhenHostLeavesWithRemainingMembers() {
        SupabaseAuthUser host = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        GameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");
        UUID promotedLearnerId = UUID.randomUUID();
        GameLobbyMember promoted = existingMember(lobby, promotedLearnerId, "2026-04-01T12:05:00Z");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, host.userId()))
                .thenReturn(Optional.of(hostMember));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(promoted), List.of(promoted));
        when(learnerRepository.findAllById(List.of(promotedLearnerId)))
                .thenReturn(List.of(learner(promotedLearnerId, "new-host")));

        LeavePrivateGameLobbyResponse result = service.leavePrivateLobby(host, lobbyPublicId);

        assertThat(result.result()).isEqualTo(PrivateGameLobbyLeaveResult.LEFT_AND_PROMOTED_HOST);
        assertThat(result.lobbyState()).isNotNull();
        assertThat(lobby.getHostLearnerId()).isEqualTo(promotedLearnerId);
        assertThat(result.lobbyState().activeMemberCount()).isEqualTo(1);
    }

    @Test
    void leavePrivateLobbyDeletesLobbyWhenHostLeavesAndNoMembersRemain() {
        SupabaseAuthUser host = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());
        GameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, host.userId()))
                .thenReturn(Optional.of(hostMember));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of());

        LeavePrivateGameLobbyResponse result = service.leavePrivateLobby(host, lobbyPublicId);

        assertThat(result.result()).isEqualTo(PrivateGameLobbyLeaveResult.LEFT_AND_LOBBY_DELETED);
        assertThat(result.lobbyState()).isNull();
        verify(imposterGameLobbyMemberRepository).delete(hostMember);
        verify(imposterGameLobbyRepository).deleteById(11L);
    }

    @Test
    void leavePrivateLobbyAbandonsSessionWhenAnyActiveMemberLeavesAfterStart() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setHostLearnerId(user.userId());

        UUID remainingLearnerId = UUID.randomUUID();
        GameLobbyMember self = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember remaining = existingMember(lobby, remainingLearnerId, "2026-04-01T12:05:00Z");
        Learner remainingLearner = learner(remainingLearnerId, "remaining");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, user.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.of(self));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(remaining), List.of(remaining));
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(remainingLearner, learner(user.userId(), "quitter")));

        LeavePrivateGameLobbyResponse response = service.leavePrivateLobby(user, lobbyPublicId);

        assertThat(response.result()).isEqualTo(PrivateGameLobbyLeaveResult.LEFT_AND_SESSION_ABANDONED);
        assertThat(response.lobbyState()).isNotNull();
        assertThat(response.lobbyState().activeMemberCount()).isEqualTo(1);
        assertThat(response.lobbyState().currentPhase())
                .isEqualTo(com.example.demo.game.lobby.GameLobbyPhase.ABANDONED);
        assertThat(response.lobbyState().endReason()).isEqualTo("PLAYER_QUIT");
        assertThat(response.lobbyState().endedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(response.lobbyState().endedByPublicId()).isNotNull();
        verify(imposterLobbyRealtimePublisher).publishSharedLobbyState(eq(lobbyPublicId), eq(1), eq("ABANDONED_BY_QUIT"), any());
        verify(imposterLobbyRealtimePublisher).publishViewerLobbyState(eq(lobbyPublicId), any(), eq(1), eq("ABANDONED_BY_QUIT"), any());
    }

    @Test
    void leavePrivateLobbyDoesNotDeleteAbandonedLobbyWhenLastMemberLeaves() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.ABANDONED);

        GameLobbyMember self = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, user.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.of(self));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(), List.of());
        when(learnerRepository.findAllById(List.of())).thenReturn(List.of());

        LeavePrivateGameLobbyResponse response = service.leavePrivateLobby(user, lobbyPublicId);

        assertThat(response.result()).isEqualTo(PrivateGameLobbyLeaveResult.LEFT);
        assertThat(response.lobbyState()).isNotNull();
        assertThat(response.lobbyState().activeMemberCount()).isEqualTo(0);
        verify(imposterGameLobbyRepository, never()).deleteById(anyLong());
    }

    @Test
    void submitVoteRejectsWhenLobbySessionAbandoned() {
        SupabaseAuthUser voter = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.ABANDONED);

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, voter.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.submitVote(
                voter,
                lobbyPublicId,
                new SubmitGameVoteRequest(UUID.randomUUID())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("session has been abandoned");

        verify(imposterGameLobbyRepository, never()).saveAndFlush(any(GameLobby.class));
    }

    @Test
    void processRealtimeDisconnectTimeoutsAbandonsLobbyAndPublishesRealtime() {
        UUID disconnectedLearnerId = UUID.randomUUID();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setStateVersion(7);

        GameLobbyMember disconnectedMember = existingMember(lobby, disconnectedLearnerId, "2026-04-01T12:00:00Z");
        GameLobbyMember otherMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:05:00Z");
        when(imposterLobbyRealtimePresenceTracker.consumeDueDisconnectTimeouts(any()))
                .thenReturn(List.of(new GameLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate(
                        lobbyPublicId,
                        disconnectedLearnerId,
                        OffsetDateTime.parse("2026-04-02T00:00:00Z")
                )));
        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, disconnectedLearnerId))
                .thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(disconnectedMember, otherMember), List.of(disconnectedMember, otherMember));
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        learner(disconnectedLearnerId, "dc"),
                        learner(otherMember.getLearnerId(), "other")
                ));

        service.processRealtimeDisconnectTimeouts();

        assertThat(lobby.getCurrentPhase()).isEqualTo(com.example.demo.game.lobby.GameLobbyPhase.ABANDONED);
        assertThat(lobby.getEndedReason()).isEqualTo("PLAYER_DISCONNECTED_TIMEOUT");
        assertThat(lobby.getEndedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(lobby.getAbandonedByLearnerId()).isEqualTo(disconnectedLearnerId);
        verify(imposterLobbyRealtimePublisher)
                .publishSharedLobbyState(eq(lobbyPublicId), eq(8), eq("ABANDONED_BY_DISCONNECT_TIMEOUT"), any());
    }

    @Test
    void processRealtimeDisconnectTimeoutsSkipsLobbyWhenNotStarted() {
        UUID disconnectedLearnerId = UUID.randomUUID();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        when(imposterLobbyRealtimePresenceTracker.consumeDueDisconnectTimeouts(any()))
                .thenReturn(List.of(new GameLobbyRealtimePresenceTracker.DisconnectTimeoutCandidate(
                        lobbyPublicId,
                        disconnectedLearnerId,
                        OffsetDateTime.parse("2026-04-02T00:00:00Z")
                )));
        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));

        service.processRealtimeDisconnectTimeouts();

        verify(imposterLobbyRealtimePublisher, never()).publishSharedLobbyState(any(), any(), any(), any());
    }

    @Test
    void startPrivateLobbySucceedsWhenHostAndEnoughActiveMembers() {
        SupabaseAuthUser host = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        GameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember secondMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:05:00Z");
        GameLobbyMember thirdMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:10:00Z");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, host.userId()))
                .thenReturn(Optional.of(hostMember));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(hostMember, secondMember, thirdMember));
        when(learnerRepository.findAllById(List.of(host.userId(), secondMember.getLearnerId(), thirdMember.getLearnerId())))
                .thenReturn(List.of(
                        learner(host.userId(), "host"),
                        learner(secondMember.getLearnerId(), "member-2"),
                        learner(thirdMember.getLearnerId(), "member-3")
                ));

        PrivateGameLobbyStateDto result = service.startPrivateLobby(host, lobbyPublicId);

        assertThat(lobby.getStartedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(lobby.getStartedByLearnerId()).isEqualTo(host.userId());
        assertThat(result.startedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(result.canStart()).isFalse();
    }

    @Test
    void startPrivateLobbyRejectsWhenInsufficientPlayers() {
        SupabaseAuthUser host = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, host.userId()))
                .thenReturn(Optional.of(existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z")));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(
                        existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z"),
                        existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:05:00Z")
                ));

        assertThatThrownBy(() -> service.startPrivateLobby(host, lobbyPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("At least 3 active players are required");
    }

    @Test
    void startPrivateLobbyRejectsWhenRequesterIsNotHost() {
        SupabaseAuthUser requester = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(UUID.randomUUID());

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, requester.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.startPrivateLobby(requester, lobbyPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only lobby host can start");
    }

    @Test
    void getPrivateLobbyStateReturnsLiveSnapshot() {
        SupabaseAuthUser host = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        GameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember secondMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:05:00Z");
        GameLobbyMember thirdMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:10:00Z");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(hostMember, secondMember, thirdMember));
        when(learnerRepository.findAllById(List.of(host.userId(), secondMember.getLearnerId(), thirdMember.getLearnerId())))
                .thenReturn(List.of(
                        learner(host.userId(), "host"),
                        learner(secondMember.getLearnerId(), "member-2"),
                        learner(thirdMember.getLearnerId(), "member-3")
                ));

        PrivateGameLobbyStateDto result = service.getPrivateLobbyState(host, lobbyPublicId);

        assertThat(result.activeMemberCount()).isEqualTo(3);
        assertThat(result.viewerIsHost()).isTrue();
        assertThat(result.viewerIsActiveMember()).isTrue();
        assertThat(result.canLeave()).isTrue();
        assertThat(result.canStart()).isTrue();
    }

    @Test
    void getPrivateLobbyStateIncludesReconnectPresenceCountdown() {
        SupabaseAuthUser host = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        GameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");
        UUID reconnectingLearnerId = UUID.randomUUID();
        GameLobbyMember reconnectingMember = existingMember(lobby, reconnectingLearnerId, "2026-04-01T12:05:00Z");

        Learner hostLearner = learner(host.userId(), "host");
        Learner reconnectingLearner = learner(reconnectingLearnerId, "reconnect");
        OffsetDateTime deadline = OffsetDateTime.parse("2026-04-02T00:00:30Z");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(hostMember, reconnectingMember));
        when(learnerRepository.findAllById(anyList())).thenReturn(List.of(hostLearner, reconnectingLearner));
        when(imposterLobbyRealtimePresenceTracker.listReconnectPresence(lobbyPublicId))
                .thenReturn(List.of(new GameLobbyRealtimePresenceTracker.ReconnectPresenceSnapshot(reconnectingLearnerId, deadline)));

        PrivateGameLobbyStateDto result = service.getPrivateLobbyState(host, lobbyPublicId);

        assertThat(result.reconnectingLearners()).hasSize(1);
        assertThat(result.reconnectingLearners().get(0).learnerPublicId()).isEqualTo(reconnectingLearner.getPublicId());
        assertThat(result.reconnectingLearners().get(0).disconnectDeadlineAt()).isEqualTo(deadline);
    }

    @Test
    void publishRealtimePresenceUpdateBroadcastsSharedAndViewerState() {
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        UUID memberOneId = UUID.randomUUID();
        UUID memberTwoId = UUID.randomUUID();
        GameLobbyMember memberOne = existingMember(lobby, memberOneId, "2026-04-01T12:00:00Z");
        GameLobbyMember memberTwo = existingMember(lobby, memberTwoId, "2026-04-01T12:05:00Z");

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(memberOne, memberTwo));
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(learner(memberOneId, "one"), learner(memberTwoId, "two")));

        service.publishRealtimePresenceUpdate(lobbyPublicId, "RECONNECTING");

        verify(imposterLobbyRealtimePublisher).publishSharedLobbyState(eq(lobbyPublicId), any(), eq("RECONNECTING"), any());
        verify(imposterLobbyRealtimePublisher, times(2)).publishViewerLobbyState(eq(lobbyPublicId), any(), any(), eq("RECONNECTING"), any());
    }

    @Test
    void submitVoteRejectsSelfVote() {
        SupabaseAuthUser voter = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.VOTING);
        lobby.setVotingRoundNumber(1);
        lobby.setVotingDeadlineAt(OffsetDateTime.parse("2026-04-02T00:10:00Z"));

        UUID otherLearnerId1 = UUID.randomUUID();
        UUID otherLearnerId2 = UUID.randomUUID();
        lobby.setVotingEligibleTargetLearnerIds(voter.userId() + "," + otherLearnerId1 + "," + otherLearnerId2);

        GameLobbyMember voterMember = existingMember(lobby, voter.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember otherMember1 = existingMember(lobby, otherLearnerId1, "2026-04-01T12:05:00Z");
        GameLobbyMember otherMember2 = existingMember(lobby, otherLearnerId2, "2026-04-01T12:10:00Z");

        Learner voterLearner = learner(voter.userId(), "voter");
        Learner otherLearner1 = learner(otherLearnerId1, "other-1");
        Learner otherLearner2 = learner(otherLearnerId2, "other-2");

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, voter.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(voterMember, otherMember1, otherMember2));
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(voterLearner, otherLearner1, otherLearner2));

        assertThatThrownBy(() -> service.submitVote(
                voter,
                lobbyPublicId,
                new SubmitGameVoteRequest(voterLearner.getPublicId())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot vote for yourself");

        verify(imposterGameLobbyRepository, never()).saveAndFlush(any(GameLobby.class));
    }

    @Test
    void submitVoteResolvesTieAtMaxVotingRoundsAndKeepsViewerVoteSelectionInState() {
        UUID voterOneId = UUID.randomUUID();
        UUID voterTwoId = UUID.randomUUID();

        Learner voterOneLearner = learner(voterOneId, "voter-one");
        Learner voterTwoLearner = learner(voterTwoId, "voter-two");
        SupabaseAuthUser voterOne = new SupabaseAuthUser(voterOneId, voterOneLearner, null);
        SupabaseAuthUser voterTwo = new SupabaseAuthUser(voterTwoId, voterTwoLearner, null);

        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.VOTING);
        lobby.setVotingRoundNumber(1);
        lobby.setMaxVotingRounds(1);
        lobby.setVotingDeadlineAt(OffsetDateTime.parse("2026-04-02T00:10:00Z"));
        lobby.setCurrentGameLearnerId(voterOneId);
        lobby.setCurrentConceptIndex(1);
        lobby.setCurrentConceptTitle("apple");
        lobby.setVotingEligibleTargetLearnerIds(voterOneId + "," + voterTwoId);

        GameLobbyMember voterOneMember = existingMember(lobby, voterOneId, "2026-04-01T12:00:00Z");
        GameLobbyMember voterTwoMember = existingMember(lobby, voterTwoId, "2026-04-01T12:05:00Z");
        List<GameLobbyMember> activeMembers = List.of(voterOneMember, voterTwoMember);

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, voterOneId)).thenReturn(true);
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, voterTwoId)).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(activeMembers);
        when(learnerRepository.findAllById(anyList())).thenReturn(List.of(voterOneLearner, voterTwoLearner));

        PrivateGameLobbyStateDto firstVoteState = service.submitVote(
                voterOne,
                lobbyPublicId,
                new SubmitGameVoteRequest(voterTwoLearner.getPublicId())
        );

        assertThat(firstVoteState.viewerVoteTargetPublicId()).isEqualTo(voterTwoLearner.getPublicId());

        PrivateGameLobbyStateDto secondVoteState = service.submitVote(
                voterTwo,
                lobbyPublicId,
                new SubmitGameVoteRequest(voterOneLearner.getPublicId())
        );

        assertThat(lobby.getCurrentPhase()).isEqualTo(com.example.demo.game.lobby.GameLobbyPhase.CONCEPT_RESULT);
        assertThat(lobby.getLatestResultResolution()).isEqualTo("VOTING_TIE_LIMIT");
        assertThat(lobby.getLatestResultWinnerSide()).isEqualTo("IMPOSTER");
        assertThat(secondVoteState.currentPhase()).isEqualTo(com.example.demo.game.lobby.GameLobbyPhase.CONCEPT_RESULT);
        assertThat(secondVoteState.latestConceptResult()).isNotNull();
        assertThat(secondVoteState.latestConceptResult().resolution().name()).isEqualTo("VOTING_TIE_LIMIT");
    }

    @Test
    void submitDrawingDoneCommitsSnapshotAdvancesTurnAndPublishesRealtime() {
        SupabaseAuthUser drawer = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(drawer.userId());
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setCurrentDrawerLearnerId(drawer.userId());
        lobby.setCurrentTurnIndex(0);
        lobby.setRoundNumber(1);
        lobby.setRoundsPerConcept(1);
        lobby.setTurnDurationSeconds(25);
        lobby.setTurnStartedAt(OffsetDateTime.parse("2026-04-01T23:59:50Z"));
        lobby.setTurnEndsAt(OffsetDateTime.parse("2026-04-02T00:00:10Z"));
        lobby.setDrawingVersion(2);
        lobby.setStateVersion(3);

        UUID memberTwoId = UUID.randomUUID();
        UUID memberThreeId = UUID.randomUUID();
        lobby.setRoundDrawerOrder(drawer.userId() + "," + memberTwoId);
        GameLobbyMember drawerMember = existingMember(lobby, drawer.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember memberTwo = existingMember(lobby, memberTwoId, "2026-04-01T12:05:00Z");
        GameLobbyMember memberThree = existingMember(lobby, memberThreeId, "2026-04-01T12:10:00Z");
        List<GameLobbyMember> activeMembers = List.of(drawerMember, memberTwo, memberThree);

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, drawer.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L)).thenReturn(activeMembers);
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        learner(drawer.userId(), "drawer"),
                        learner(memberTwoId, "member-2"),
                        learner(memberThreeId, "member-3")
                ));

        PrivateGameLobbyStateDto state = service.submitDrawingDone(
                drawer,
                lobbyPublicId,
                new UpsertGameDrawingSnapshotRequest("{\"final\":true}", 2)
        );

        assertThat(state.currentTurnIndex()).isEqualTo(1);
        assertThat(state.stateVersion()).isEqualTo(4);
        assertThat(lobby.getCurrentDrawingSnapshot()).isEqualTo("{\"final\":true}");
        assertThat(lobby.getDrawingVersion()).isEqualTo(3);
        assertThat(lobby.getStateVersion()).isEqualTo(4);
        verify(imposterLobbyRealtimePublisher).publishSharedLobbyState(eq(lobbyPublicId), eq(4), eq("DRAWING_DONE"), any());
        verify(imposterLobbyRealtimePublisher, times(3)).publishViewerLobbyState(eq(lobbyPublicId), any(), eq(4), eq("DRAWING_DONE"), any());
    }

    @Test
    void submitDrawingDoneRejectsStaleBaseVersion() {
        SupabaseAuthUser drawer = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setCurrentDrawerLearnerId(drawer.userId());
        lobby.setDrawingVersion(3);
        lobby.setTurnEndsAt(OffsetDateTime.parse("2026-04-02T00:00:10Z"));

        GameLobbyMember drawerMember = existingMember(lobby, drawer.userId(), "2026-04-01T12:00:00Z");
        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, drawer.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(drawerMember));

        assertThatThrownBy(() -> service.submitDrawingDone(
                drawer,
                lobbyPublicId,
                new UpsertGameDrawingSnapshotRequest("{\"final\":true}", 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Drawing version conflict");

        verify(imposterGameLobbyRepository, never()).saveAndFlush(any(GameLobby.class));
    }

    @Test
    void submitDrawingDoneRejectsWhenViewerIsNotCurrentDrawer() {
        SupabaseAuthUser user = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setCurrentDrawerLearnerId(UUID.randomUUID());

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, user.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.submitDrawingDone(
                user,
                lobbyPublicId,
                new UpsertGameDrawingSnapshotRequest("{\"final\":true}", 0)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only current drawer can perform this action");

        verify(imposterGameLobbyRepository, never()).saveAndFlush(any(GameLobby.class));
    }

    @Test
    void submitLiveDrawingRejectsWhenDoneBasedModeIsEnabled() {
        SupabaseAuthUser drawer = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setCurrentDrawerLearnerId(drawer.userId());

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, drawer.userId())).thenReturn(true);
        assertThatThrownBy(() -> service.submitLiveDrawing(
                drawer,
                lobbyPublicId,
                new UpsertGameDrawingSnapshotRequest("{\"live\":true}", 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Live drawing is disabled");

        verify(imposterGameLobbyRepository, never()).saveAndFlush(any(GameLobby.class));
        verify(imposterLobbyRealtimePublisher, never()).publishSharedLobbyState(any(), any(), any(), any());
        verify(imposterLobbyRealtimePublisher, never()).publishViewerLobbyState(any(), any(), any(), any(), any());
    }

    @Test
    void submitLiveDrawingCommitsSnapshotWhenLiveModeEnabled() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC);
        LearnerGameLobbyService liveDrawingService = new LearnerGameLobbyService(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                imposterLobbyCodeGenerator,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                gameLobbyInviteRepository,
                imposterWeeklyFeaturedConceptService,
                conceptRepository,
                learnerRepository,
                imposterLobbyRealtimePublisher,
                imposterLobbyRealtimePresenceTracker,
                fixedClock,
                true
        );

        SupabaseAuthUser drawer = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(drawer.userId());
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setCurrentDrawerLearnerId(drawer.userId());
        lobby.setCurrentTurnIndex(0);
        lobby.setRoundNumber(1);
        lobby.setRoundsPerConcept(1);
        lobby.setTurnDurationSeconds(25);
        lobby.setTurnStartedAt(OffsetDateTime.parse("2026-04-01T23:59:50Z"));
        lobby.setTurnEndsAt(OffsetDateTime.parse("2026-04-02T00:00:10Z"));
        lobby.setDrawingVersion(2);
        lobby.setStateVersion(3);

        UUID memberTwoId = UUID.randomUUID();
        UUID memberThreeId = UUID.randomUUID();
        lobby.setRoundDrawerOrder(drawer.userId() + "," + memberTwoId);
        GameLobbyMember drawerMember = existingMember(lobby, drawer.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember memberTwo = existingMember(lobby, memberTwoId, "2026-04-01T12:05:00Z");
        GameLobbyMember memberThree = existingMember(lobby, memberThreeId, "2026-04-01T12:10:00Z");
        List<GameLobbyMember> activeMembers = List.of(drawerMember, memberTwo, memberThree);

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, drawer.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L)).thenReturn(activeMembers);
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        learner(drawer.userId(), "drawer"),
                        learner(memberTwoId, "member-2"),
                        learner(memberThreeId, "member-3")
                ));

        PrivateGameLobbyStateDto state = liveDrawingService.submitLiveDrawing(
                drawer,
                lobbyPublicId,
                new UpsertGameDrawingSnapshotRequest("{\"live\":true}", 2)
        );

        assertThat(state.currentTurnIndex()).isEqualTo(0);
        assertThat(state.currentPhase()).isEqualTo(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        assertThat(state.stateVersion()).isEqualTo(4);
        assertThat(lobby.getCurrentDrawingSnapshot()).isEqualTo("{\"live\":true}");
        assertThat(lobby.getDrawingVersion()).isEqualTo(3);
        assertThat(lobby.getStateVersion()).isEqualTo(4);
        verify(imposterLobbyRealtimePublisher).publishSharedLobbyState(eq(lobbyPublicId), eq(4), eq("DRAWING_LIVE"), any());
        verify(imposterLobbyRealtimePublisher, times(3)).publishViewerLobbyState(eq(lobbyPublicId), any(), eq(4), eq("DRAWING_LIVE"), any());
    }

    @Test
    void submitDrawingDoneRejectsWhenTurnAlreadyExpiredAndPublishesAuthoritativeState() {
        SupabaseAuthUser drawer = learnerAuthUser();
        GameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(drawer.userId());
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));
        lobby.setCurrentPhase(com.example.demo.game.lobby.GameLobbyPhase.DRAWING);
        lobby.setCurrentDrawerLearnerId(drawer.userId());
        lobby.setCurrentTurnIndex(0);
        lobby.setRoundNumber(1);
        lobby.setRoundsPerConcept(1);
        lobby.setTurnDurationSeconds(25);
        lobby.setTurnStartedAt(OffsetDateTime.parse("2026-04-01T23:59:30Z"));
        lobby.setTurnEndsAt(OffsetDateTime.parse("2026-04-01T23:59:59Z"));
        lobby.setDrawingVersion(1);
        lobby.setStateVersion(10);

        UUID memberTwoId = UUID.randomUUID();
        UUID memberThreeId = UUID.randomUUID();
        lobby.setRoundDrawerOrder(drawer.userId() + "," + memberTwoId);
        GameLobbyMember drawerMember = existingMember(lobby, drawer.userId(), "2026-04-01T12:00:00Z");
        GameLobbyMember memberTwo = existingMember(lobby, memberTwoId, "2026-04-01T12:05:00Z");
        GameLobbyMember memberThree = existingMember(lobby, memberThreeId, "2026-04-01T12:10:00Z");
        List<GameLobbyMember> activeMembers = List.of(drawerMember, memberTwo, memberThree);

        when(imposterGameLobbyRepository.findByPublicIdForUpdate(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, drawer.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L)).thenReturn(activeMembers, activeMembers);
        when(learnerRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        learner(drawer.userId(), "drawer"),
                        learner(memberTwoId, "member-2"),
                        learner(memberThreeId, "member-3")
                ));

        assertThatThrownBy(() -> service.submitDrawingDone(
                drawer,
                lobbyPublicId,
                new UpsertGameDrawingSnapshotRequest("{\"final\":true}", 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Drawing turn has ended");

        assertThat(lobby.getStateVersion()).isEqualTo(11);
        verify(imposterLobbyRealtimePublisher).publishSharedLobbyState(eq(lobbyPublicId), eq(11), eq("TURN_EXPIRED"), any());
        verify(imposterLobbyRealtimePublisher, times(3)).publishViewerLobbyState(eq(lobbyPublicId), any(), eq(11), eq("TURN_EXPIRED"), any());
    }

    @Test
    void joinPrivateLobbyRejectsWhenUserIsNotLearner() {
        SupabaseAuthUser notLearner = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> service.joinPrivateLobby(
                notLearner,
                new JoinPrivateGameLobbyRequest("ABCD2345")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Learner account required");
    }

    private SupabaseAuthUser learnerAuthUser() {
        UUID userId = UUID.randomUUID();
        Learner learner = new Learner();
        learner.setId(userId);
        learner.setPublicId(UUID.randomUUID());
        learner.setUsername("learner-" + userId.toString().substring(0, 8));
        learner.setCreatedAt(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        learner.setTotalPoints((short) 0);
        return new SupabaseAuthUser(userId, learner, null);
    }

    private Learner learner(UUID id, String username) {
        Learner learner = new Learner();
        learner.setId(id);
        learner.setPublicId(UUID.randomUUID());
        learner.setUsername(username);
        learner.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        learner.setTotalPoints((short) 0);
        return learner;
    }

    private GameLobby lobby(String lobbyCode) {
        GameLobby lobby = new GameLobby();
        ReflectionTestUtils.setField(lobby, "id", 11L);
        ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
        lobby.setLobbyCode(lobbyCode);
        lobby.setPrivateLobby(true);
        lobby.setConceptPoolMode(GameLobbyConceptPoolMode.FULL_CONCEPT_POOL);
        lobby.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        lobby.setHostLearnerId(UUID.randomUUID());
        return lobby;
    }

    private GameLobbyMember existingMember(GameLobby lobby, UUID learnerId, String joinedAt) {
        GameLobbyMember member = new GameLobbyMember();
        member.setLobby(lobby);
        member.setLearnerId(learnerId);
        member.setJoinedAt(OffsetDateTime.parse(joinedAt));
        return member;
    }
}
