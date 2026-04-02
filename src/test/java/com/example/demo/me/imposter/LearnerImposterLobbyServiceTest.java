package com.example.demo.me.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyCodeGenerator;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyStateDto;
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
class LearnerImposterLobbyServiceTest {

    @Mock
    private ImposterGameLobbyRepository imposterGameLobbyRepository;

    @Mock
    private ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;

    @Mock
    private ImposterLobbyCodeGenerator imposterLobbyCodeGenerator;

    @Mock
    private ImposterMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    @Mock
    private LearnerRepository learnerRepository;

    private LearnerImposterLobbyService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC);
        service = new LearnerImposterLobbyService(
                imposterGameLobbyRepository,
                imposterGameLobbyMemberRepository,
                imposterLobbyCodeGenerator,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                learnerRepository,
                fixedClock
        );
        lenient().when(imposterGameLobbyRepository.saveAndFlush(any(ImposterGameLobby.class))).thenAnswer(invocation -> {
            ImposterGameLobby lobby = invocation.getArgument(0);
            if (lobby.getPublicId() == null) {
                ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
            }
            return lobby;
        });
        lenient().when(imposterGameLobbyMemberRepository.saveAndFlush(any(ImposterGameLobbyMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(imposterLobbyCodeGenerator.generate()).thenReturn("ABCD2345");
    }

    @Test
    void createsPrivateLobbyForFullConceptPool() {
        SupabaseAuthUser user = learnerAuthUser();

        PrivateImposterLobbyDto result = service.createPrivateLobby(
                user,
                new CreatePrivateImposterLobbyRequest(ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL)
        );

        assertThat(result.publicId()).isNotNull();
        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        assertThat(result.isPrivate()).isTrue();
        assertThat(result.conceptPoolMode()).isEqualTo(ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL);
        assertThat(result.pinnedYearMonth()).isNull();
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        verify(imposterGameLobbyMemberRepository).saveAndFlush(any(ImposterGameLobbyMember.class));
    }

    @Test
    void createsPrivateLobbyForCurrentMonthPackWhenConfigured() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 91L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        ImposterMonthlyPackConcept conceptRow = new ImposterMonthlyPackConcept();
        conceptRow.setPack(pack);
        conceptRow.setSlotIndex((short) 1);
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(91L))
                .thenReturn(List.of(conceptRow));

        PrivateImposterLobbyDto result = service.createPrivateLobby(
                user,
                new CreatePrivateImposterLobbyRequest(ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK)
        );

        assertThat(result.publicId()).isNotNull();
        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        assertThat(result.conceptPoolMode()).isEqualTo(ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK);
        assertThat(result.pinnedYearMonth()).isEqualTo("2026-04");
    }

    @Test
    void rejectsCurrentMonthPackLobbyWhenPackMissing() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPrivateLobby(
                user,
                new CreatePrivateImposterLobbyRequest(ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current monthly imposter pack is not configured");
    }

    @Test
    void rejectsCurrentMonthPackLobbyWhenPackHasNoConcepts() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 91L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(91L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createPrivateLobby(
                user,
                new CreatePrivateImposterLobbyRequest(ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current monthly imposter pack has no concepts");
    }

    @Test
    void retriesLobbyCodeGenerationWhenUniqueCollisionOccurs() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterLobbyCodeGenerator.generate()).thenReturn("ABCD2345", "WXYZ6789");
        when(imposterGameLobbyRepository.saveAndFlush(any(ImposterGameLobby.class)))
                .thenThrow(new DataIntegrityViolationException("violates constraint uk_imposter_game_lobbies_lobby_code"))
                .thenAnswer(invocation -> {
                    ImposterGameLobby lobby = invocation.getArgument(0);
                    ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
                    return lobby;
                });

        PrivateImposterLobbyDto result = service.createPrivateLobby(
                user,
                new CreatePrivateImposterLobbyRequest(ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL)
        );

        assertThat(result.lobbyCode()).isEqualTo("WXYZ6789");
        verify(imposterLobbyCodeGenerator, times(2)).generate();
        verify(imposterGameLobbyRepository, times(2)).saveAndFlush(any(ImposterGameLobby.class));
    }

    @Test
    void createPrivateLobbyPropagatesWhenHostMembershipInsertFails() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterGameLobbyMemberRepository.saveAndFlush(any(ImposterGameLobbyMember.class)))
                .thenThrow(new RuntimeException("membership insert failed"));

        assertThatThrownBy(() -> service.createPrivateLobby(
                user,
                new CreatePrivateImposterLobbyRequest(ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL)
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("membership insert failed");
    }

    @Test
    void joinPrivateLobbyCreatesMembershipForValidCode() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.empty());
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(11L, user.userId()))
                .thenReturn(Optional.empty());

        JoinedPrivateImposterLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateImposterLobbyRequest("ABCD2345")
        );

        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        assertThat(result.alreadyMember()).isFalse();
        assertThat(result.joinedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        verify(imposterGameLobbyMemberRepository).saveAndFlush(any(ImposterGameLobbyMember.class));
    }

    @Test
    void joinPrivateLobbyNormalizesLobbyCode() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.empty());
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(11L, user.userId()))
                .thenReturn(Optional.empty());

        JoinedPrivateImposterLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateImposterLobbyRequest("  abcd2345  ")
        );

        assertThat(result.lobbyCode()).isEqualTo("ABCD2345");
        verify(imposterGameLobbyRepository).findByLobbyCode(eq("ABCD2345"));
    }

    @Test
    void joinPrivateLobbyReturnsConflictWhenAlreadyActiveMember() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        ImposterGameLobbyMember member = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.joinPrivateLobby(
                user,
                new JoinPrivateImposterLobbyRequest("ABCD2345")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Learner already joined this lobby");
        verify(imposterGameLobbyMemberRepository, never()).saveAndFlush(any(ImposterGameLobbyMember.class));
    }

    @Test
    void joinPrivateLobbyAllowsRejoinAfterLearnerHasLeft() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        ImposterGameLobbyMember historical = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        historical.setLeftAt(OffsetDateTime.parse("2026-04-01T13:00:00Z"));
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.empty());
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(11L, user.userId()))
                .thenReturn(Optional.of(historical));

        JoinedPrivateImposterLobbyDto result = service.joinPrivateLobby(
                user,
                new JoinPrivateImposterLobbyRequest("ABCD2345")
        );

        assertThat(result.alreadyMember()).isFalse();
        assertThat(result.joinedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(historical.getLeftAt()).isNull();
        verify(imposterGameLobbyMemberRepository).saveAndFlush(historical);
    }

    @Test
    void joinPrivateLobbyReturnsNotFoundWhenLobbyCodeMissing() {
        SupabaseAuthUser user = learnerAuthUser();
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinPrivateLobby(
                user,
                new JoinPrivateImposterLobbyRequest("ABCD2345")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Imposter lobby not found");
    }

    @Test
    void joinPrivateLobbyRejectsMissingLobbyCode() {
        SupabaseAuthUser user = learnerAuthUser();

        assertThatThrownBy(() -> service.joinPrivateLobby(
                user,
                new JoinPrivateImposterLobbyRequest("   ")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("lobbyCode is required");
    }

    @Test
    void leavePrivateLobbyMarksActiveMemberLeftWhenLobbyNotStarted() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        ImposterGameLobbyMember self = existingMember(lobby, user.userId(), "2026-04-01T12:00:00Z");
        ImposterGameLobbyMember other = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:30:00Z");
        Learner otherLearner = learner(other.getLearnerId(), "peer");

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, user.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, user.userId()))
                .thenReturn(Optional.of(self));
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(other));
        when(learnerRepository.findAllById(List.of(other.getLearnerId()))).thenReturn(List.of(otherLearner));

        PrivateImposterLobbyStateDto result = service.leavePrivateLobby(user, lobbyPublicId);

        assertThat(self.getLeftAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(result.activeMemberCount()).isEqualTo(1);
        assertThat(result.viewerIsActiveMember()).isFalse();
        assertThat(result.canLeave()).isFalse();
    }

    @Test
    void leavePrivateLobbyRejectsWhenLobbyAlreadyStarted() {
        SupabaseAuthUser user = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-01T15:00:00Z"));

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, user.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.leavePrivateLobby(user, lobbyPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void startPrivateLobbySucceedsWhenHostAndEnoughActiveMembers() {
        SupabaseAuthUser host = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        ImposterGameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");
        ImposterGameLobbyMember secondMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:05:00Z");
        ImposterGameLobbyMember thirdMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:10:00Z");

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, host.userId()))
                .thenReturn(Optional.of(hostMember));
        when(imposterGameLobbyMemberRepository.countByLobby_IdAndLeftAtIsNull(11L)).thenReturn(3L);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(hostMember, secondMember, thirdMember));
        when(learnerRepository.findAllById(List.of(host.userId(), secondMember.getLearnerId(), thirdMember.getLearnerId())))
                .thenReturn(List.of(
                        learner(host.userId(), "host"),
                        learner(secondMember.getLearnerId(), "member-2"),
                        learner(thirdMember.getLearnerId(), "member-3")
                ));

        PrivateImposterLobbyStateDto result = service.startPrivateLobby(host, lobbyPublicId);

        assertThat(lobby.getStartedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(lobby.getStartedByLearnerId()).isEqualTo(host.userId());
        assertThat(result.startedAt()).isEqualTo(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        assertThat(result.canStart()).isFalse();
    }

    @Test
    void startPrivateLobbyRejectsWhenInsufficientPlayers() {
        SupabaseAuthUser host = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerIdAndLeftAtIsNull(11L, host.userId()))
                .thenReturn(Optional.of(existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z")));
        when(imposterGameLobbyMemberRepository.countByLobby_IdAndLeftAtIsNull(11L)).thenReturn(2L);

        assertThatThrownBy(() -> service.startPrivateLobby(host, lobbyPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("At least 3 active players are required");
    }

    @Test
    void startPrivateLobbyRejectsWhenRequesterIsNotHost() {
        SupabaseAuthUser requester = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(UUID.randomUUID());

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, requester.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.startPrivateLobby(requester, lobbyPublicId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only lobby host can start");
    }

    @Test
    void getPrivateLobbyStateReturnsLiveSnapshot() {
        SupabaseAuthUser host = learnerAuthUser();
        ImposterGameLobby lobby = lobby("ABCD2345");
        UUID lobbyPublicId = lobby.getPublicId();
        lobby.setHostLearnerId(host.userId());

        ImposterGameLobbyMember hostMember = existingMember(lobby, host.userId(), "2026-04-01T12:00:00Z");
        ImposterGameLobbyMember secondMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:05:00Z");
        ImposterGameLobbyMember thirdMember = existingMember(lobby, UUID.randomUUID(), "2026-04-01T12:10:00Z");

        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));
        when(imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(11L, host.userId())).thenReturn(true);
        when(imposterGameLobbyMemberRepository.findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(11L))
                .thenReturn(List.of(hostMember, secondMember, thirdMember));
        when(learnerRepository.findAllById(List.of(host.userId(), secondMember.getLearnerId(), thirdMember.getLearnerId())))
                .thenReturn(List.of(
                        learner(host.userId(), "host"),
                        learner(secondMember.getLearnerId(), "member-2"),
                        learner(thirdMember.getLearnerId(), "member-3")
                ));

        PrivateImposterLobbyStateDto result = service.getPrivateLobbyState(host, lobbyPublicId);

        assertThat(result.activeMemberCount()).isEqualTo(3);
        assertThat(result.viewerIsHost()).isTrue();
        assertThat(result.viewerIsActiveMember()).isTrue();
        assertThat(result.canLeave()).isTrue();
        assertThat(result.canStart()).isTrue();
    }

    @Test
    void joinPrivateLobbyRejectsWhenUserIsNotLearner() {
        SupabaseAuthUser notLearner = new SupabaseAuthUser(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> service.joinPrivateLobby(
                notLearner,
                new JoinPrivateImposterLobbyRequest("ABCD2345")
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

    private ImposterGameLobby lobby(String lobbyCode) {
        ImposterGameLobby lobby = new ImposterGameLobby();
        ReflectionTestUtils.setField(lobby, "id", 11L);
        ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
        lobby.setLobbyCode(lobbyCode);
        lobby.setPrivateLobby(true);
        lobby.setConceptPoolMode(ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL);
        lobby.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        lobby.setHostLearnerId(UUID.randomUUID());
        return lobby;
    }

    private ImposterGameLobbyMember existingMember(ImposterGameLobby lobby, UUID learnerId, String joinedAt) {
        ImposterGameLobbyMember member = new ImposterGameLobbyMember();
        member.setLobby(lobby);
        member.setLearnerId(learnerId);
        member.setJoinedAt(OffsetDateTime.parse(joinedAt));
        return member;
    }
}
