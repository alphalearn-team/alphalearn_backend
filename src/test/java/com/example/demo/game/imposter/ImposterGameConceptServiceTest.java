package com.example.demo.game.imposter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.dto.ImposterAssignedConceptDto;
import com.example.demo.game.imposter.dto.NextImposterConceptRequest;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.learner.Learner;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ImposterGameConceptServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private ImposterGameLobbyRepository imposterGameLobbyRepository;

    @Mock
    private ImposterMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    @Mock
    private ImposterWeeklyFeaturedConceptService imposterWeeklyFeaturedConceptService;

    @Test
    void assignsRandomConceptOutsideExcludedSet() {
        Concept firstConcept = concept("alpha");
        Concept secondConcept = concept("beta");
        when(conceptRepository.findAll()).thenReturn(List.of(firstConcept, secondConcept));

        ImposterGameConceptService service = service();

        ImposterAssignedConceptDto result = service.assignNextConcept(
                new NextImposterConceptRequest(List.of(firstConcept.getPublicId()))
        );

        assertThat(result.conceptPublicId()).isEqualTo(secondConcept.getPublicId());
        assertThat(result.word()).isEqualTo("beta");
    }

    @Test
    void rejectsWhenNoConceptsAreAvailable() {
        when(conceptRepository.findAll()).thenReturn(List.of());

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(new NextImposterConceptRequest(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No imposter game concepts are available");
    }

    @Test
    void rejectsWhenEveryConceptIsExcluded() {
        Concept onlyConcept = concept("solo");
        when(conceptRepository.findAll()).thenReturn(List.of(onlyConcept));

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                new NextImposterConceptRequest(List.of(onlyConcept.getPublicId()))
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No imposter game concepts are available");
    }

    @Test
    void usesFullConceptPoolWhenLobbyModeIsFullPool() {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        Concept firstConcept = concept("alpha");
        Concept secondConcept = concept("beta");
        when(conceptRepository.findAll()).thenReturn(List.of(firstConcept, secondConcept));

        ImposterGameConceptService service = service();

        ImposterAssignedConceptDto result = service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(firstConcept.getPublicId()), lobbyPublicId)
        );

        assertThat(result.conceptPublicId()).isEqualTo(secondConcept.getPublicId());
        assertThat(result.word()).isEqualTo("beta");
    }

    @Test
    void usesPinnedMonthlyPackWhenLobbyModeIsCurrentMonth() {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK, "2026-04");
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 55L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept firstPackConcept = concept("delta");
        Concept secondPackConcept = concept("gamma");
        when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept("2026-04"))
                .thenReturn(Optional.empty());
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(55L)).thenReturn(List.of(
                packConcept(pack, firstPackConcept, (short) 1),
                packConcept(pack, secondPackConcept, (short) 2)
        ));

        ImposterGameConceptService service = service();

        ImposterAssignedConceptDto result = service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(firstPackConcept.getPublicId()), lobbyPublicId)
        );

        assertThat(result.conceptPublicId()).isEqualTo(secondPackConcept.getPublicId());
        assertThat(result.word()).isEqualTo("gamma");
    }

    @Test
    void usesLobbyCodeWhenProvided() {
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobby));

        Concept firstConcept = concept("alpha");
        Concept secondConcept = concept("beta");
        when(conceptRepository.findAll()).thenReturn(List.of(firstConcept, secondConcept));

        ImposterGameConceptService service = service();

        ImposterAssignedConceptDto result = service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(firstConcept.getPublicId()), null, "abcd2345")
        );

        verify(imposterGameLobbyRepository).findByLobbyCode("ABCD2345");
        assertThat(result.conceptPublicId()).isEqualTo(secondConcept.getPublicId());
        assertThat(result.word()).isEqualTo("beta");
    }

    @Test
    void rejectsWhenNonHostRequestsLobbyConcept() {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser otherUser = learnerAuthUser(UUID.randomUUID());

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                otherUser,
                new NextImposterConceptRequest(List.of(), lobbyPublicId)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only lobby host can request concepts");
    }

    @Test
    void rejectsWhenNonHostRequestsLobbyConceptByCode() {
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser otherUser = learnerAuthUser(UUID.randomUUID());

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobby));

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                otherUser,
                new NextImposterConceptRequest(List.of(), null, "ABCD2345")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only lobby host can request concepts");
    }

    @Test
    void rejectsWhenLobbyCodeAndLobbyPublicIdPointToDifferentLobbies() {
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);
        UUID lobbyPublicId = UUID.randomUUID();

        ImposterGameLobby lobbyByCode = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        ReflectionTestUtils.setField(lobbyByCode, "id", 101L);
        ImposterGameLobby lobbyByPublicId = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        ReflectionTestUtils.setField(lobbyByPublicId, "id", 202L);

        when(imposterGameLobbyRepository.findByLobbyCode("ABCD2345")).thenReturn(Optional.of(lobbyByCode));
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobbyByPublicId));

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(), lobbyPublicId, "ABCD2345")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("lobbyCode and lobbyPublicId refer to different lobbies");
    }

    @Test
    void rejectsWhenLobbyCodeIsUnknown() {
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);
        when(imposterGameLobbyRepository.findByLobbyCode("ZZZZ9999")).thenReturn(Optional.empty());

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(), null, "ZZZZ9999")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Imposter lobby not found");
    }

    @Test
    void rejectsWhenLobbyGameHasNotStarted() {
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);
        UUID lobbyPublicId = UUID.randomUUID();

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.FULL_CONCEPT_POOL, null);
        lobby.setStartedAt(null);
        lobby.setStartedByLearnerId(null);
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(), lobbyPublicId)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Imposter lobby game has not started");
    }

    @Test
    void prioritizesWeeklyFeaturedConceptForCurrentMonthLobby() {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK, "2026-04");
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        Concept featuredConcept = concept("featured");
        when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept("2026-04"))
                .thenReturn(Optional.of(featuredConcept));

        ImposterGameConceptService service = service();

        ImposterAssignedConceptDto result = service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(), lobbyPublicId)
        );

        assertThat(result.conceptPublicId()).isEqualTo(featuredConcept.getPublicId());
        assertThat(result.word()).isEqualTo(featuredConcept.getTitle());
    }

    @Test
    void fallsBackToMonthlyPoolWhenFeaturedConceptIsExcluded() {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK, "2026-04");
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 77L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept featuredConcept = concept("featured");
        Concept backupConcept = concept("backup");
        when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept("2026-04"))
                .thenReturn(Optional.of(featuredConcept));
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(77L)).thenReturn(List.of(
                packConcept(pack, featuredConcept, (short) 1),
                packConcept(pack, backupConcept, (short) 2)
        ));

        ImposterGameConceptService service = service();

        ImposterAssignedConceptDto result = service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(featuredConcept.getPublicId()), lobbyPublicId)
        );

        assertThat(result.conceptPublicId()).isEqualTo(backupConcept.getPublicId());
        assertThat(result.word()).isEqualTo(backupConcept.getTitle());
    }

    @Test
    void rejectsWhenExclusionsExhaustPinnedMonthlyPack() {
        UUID lobbyPublicId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        SupabaseAuthUser host = learnerAuthUser(hostUserId);

        ImposterGameLobby lobby = lobby(hostUserId, ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK, "2026-04");
        when(imposterGameLobbyRepository.findByPublicId(lobbyPublicId)).thenReturn(Optional.of(lobby));

        ImposterMonthlyPack pack = new ImposterMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", 99L);
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept onlyPackConcept = concept("solo-pack");
        when(imposterWeeklyFeaturedConceptService.resolveCurrentWeeklyFeaturedConcept("2026-04"))
                .thenReturn(Optional.of(onlyPackConcept));
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(99L)).thenReturn(List.of(
                packConcept(pack, onlyPackConcept, (short) 1)
        ));

        ImposterGameConceptService service = service();

        assertThatThrownBy(() -> service.assignNextConcept(
                host,
                new NextImposterConceptRequest(List.of(onlyPackConcept.getPublicId()), lobbyPublicId)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No imposter game concepts are available");
    }

    private ImposterGameConceptService service() {
        return new ImposterGameConceptService(
                conceptRepository,
                imposterGameLobbyRepository,
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                imposterWeeklyFeaturedConceptService
        );
    }

    private Concept concept(String title) {
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle(title);
        concept.setDescription(title + " description");
        concept.setCreatedAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"));
        return concept;
    }

    private ImposterGameLobby lobby(UUID hostLearnerId, ImposterLobbyConceptPoolMode mode, String pinnedYearMonth) {
        ImposterGameLobby lobby = new ImposterGameLobby();
        ReflectionTestUtils.setField(lobby, "publicId", UUID.randomUUID());
        lobby.setHostLearnerId(hostLearnerId);
        lobby.setPrivateLobby(true);
        lobby.setConceptPoolMode(mode);
        lobby.setPinnedYearMonth(pinnedYearMonth);
        lobby.setCreatedAt(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        lobby.setStartedAt(OffsetDateTime.parse("2026-04-02T00:05:00Z"));
        lobby.setStartedByLearnerId(hostLearnerId);
        return lobby;
    }

    private ImposterMonthlyPackConcept packConcept(ImposterMonthlyPack pack, Concept concept, short slot) {
        ImposterMonthlyPackConcept row = new ImposterMonthlyPackConcept();
        row.setPack(pack);
        row.setConcept(concept);
        row.setSlotIndex(slot);
        return row;
    }

    private SupabaseAuthUser learnerAuthUser(UUID userId) {
        Learner learner = new Learner();
        learner.setId(userId);
        learner.setPublicId(UUID.randomUUID());
        learner.setUsername("learner-" + userId.toString().substring(0, 8));
        learner.setCreatedAt(OffsetDateTime.parse("2026-04-02T00:00:00Z"));
        learner.setTotalPoints((short) 0);
        return new SupabaseAuthUser(userId, learner, null);
    }
}
