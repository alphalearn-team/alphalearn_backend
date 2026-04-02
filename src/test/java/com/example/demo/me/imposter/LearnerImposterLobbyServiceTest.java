package com.example.demo.me.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterLobbyCodeGenerator;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.learner.Learner;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
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
    private ImposterLobbyCodeGenerator imposterLobbyCodeGenerator;

    @Mock
    private ImposterMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    private LearnerImposterLobbyService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC);
        service = new LearnerImposterLobbyService(
                imposterGameLobbyRepository,
                imposterLobbyCodeGenerator,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                fixedClock
        );
        lenient().when(imposterGameLobbyRepository.saveAndFlush(any(ImposterGameLobby.class))).thenAnswer(invocation -> {
            ImposterGameLobby lobby = invocation.getArgument(0);
            if (lobby.getPublicId() == null) {
                ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
            }
            return lobby;
        });
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
}
