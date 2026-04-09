package com.example.demo.game.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.example.demo.game.admin.dto.AdminGameMonthlyPackDto;
import com.example.demo.game.admin.dto.UpsertAdminGameMonthlyPackRequest;
import com.example.demo.concept.Concept;
import com.example.demo.concept.ConceptRepository;
import com.example.demo.game.GameWeeklyFeaturedConceptService;
import com.example.demo.game.monthly.GameMonthlyPack;
import com.example.demo.game.monthly.GameMonthlyPackConcept;
import com.example.demo.game.monthly.GameMonthlyPackWeeklyFeature;
import com.example.demo.game.monthly.repository.GameMonthlyPackConceptRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackRepository;
import com.example.demo.game.monthly.repository.GameMonthlyPackWeeklyFeatureRepository;
import com.example.demo.quest.weekly.WeeklyQuestAssignment;
import com.example.demo.quest.weekly.WeeklyQuestAssignmentRepository;
import com.example.demo.quest.weekly.WeeklyQuestCalendarService;
import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionRepository;
import com.example.demo.quest.weekly.WeeklyQuestWeek;
import com.example.demo.quest.weekly.WeeklyQuestWeekRepository;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.quest.weekly.enums.WeeklyQuestAssignmentStatus;
import com.example.demo.quest.weekly.enums.WeeklyQuestWeekStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminGameMonthlyPackServiceTest {

    @Mock
    private GameMonthlyPackRepository imposterMonthlyPackRepository;

    @Mock
    private GameMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;

    @Mock
    private GameMonthlyPackWeeklyFeatureRepository imposterMonthlyPackWeeklyFeatureRepository;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private WeeklyQuestWeekRepository weeklyQuestWeekRepository;

    @Mock
    private WeeklyQuestAssignmentRepository weeklyQuestAssignmentRepository;

    @Mock
    private WeeklyQuestChallengeSubmissionRepository weeklyQuestChallengeSubmissionRepository;

    @Mock
    private WeeklyQuestCalendarService weeklyQuestCalendarService;

    @Mock
    private GameWeeklyFeaturedConceptService gameWeeklyFeaturedConceptService;

    private AdminGameMonthlyPackService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC);
        service = new AdminGameMonthlyPackService(
                imposterMonthlyPackRepository,
                imposterMonthlyPackConceptRepository,
                imposterMonthlyPackWeeklyFeatureRepository,
                conceptRepository,
                weeklyQuestWeekRepository,
                weeklyQuestAssignmentRepository,
                weeklyQuestChallengeSubmissionRepository,
                weeklyQuestCalendarService,
                gameWeeklyFeaturedConceptService,
                fixedClock
        );
        lenient().when(gameWeeklyFeaturedConceptService.currentWeekSlotInMonth()).thenReturn((short) 1);
        lenient().when(weeklyQuestCalendarService.currentWeekStartAt())
                .thenReturn(OffsetDateTime.parse("2026-03-29T00:00:00+08:00"));
        lenient().when(weeklyQuestWeekRepository.findByWeekStartAt(any())).thenReturn(Optional.empty());
    }

    @Test
    void returnsScaffoldWhenPackDoesNotExist() {
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        AdminGameMonthlyPackDto result = service.getMonthlyPack("2026-04");

        assertThat(result.yearMonth()).isEqualTo("2026-04");
        assertThat(result.exists()).isFalse();
        assertThat(result.concepts()).isEmpty();
        assertThat(result.weeklyFeaturedConceptPublicIds()).isEmpty();
    }

    @Test
    void returnsOrderedConceptsAndWeeklyFeaturesWhenPackExists() {
        GameMonthlyPack pack = pack(88L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(pack));

        Concept conceptOne = concept("alpha");
        Concept conceptTwo = concept("beta");
        when(imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(88L)).thenReturn(List.of(
                conceptRow(pack, conceptOne, (short) 1),
                conceptRow(pack, conceptTwo, (short) 2)
        ));

        when(imposterMonthlyPackWeeklyFeatureRepository.findByPack_IdOrderByWeekSlotAsc(88L)).thenReturn(List.of(
                weeklyFeature(pack, conceptTwo, (short) 1),
                weeklyFeature(pack, conceptOne, (short) 2)
        ));

        AdminGameMonthlyPackDto result = service.getMonthlyPack("2026-04");

        assertThat(result.exists()).isTrue();
        assertThat(result.concepts()).hasSize(2);
        assertThat(result.concepts().get(0).slotIndex()).isEqualTo((short) 1);
        assertThat(result.concepts().get(0).conceptPublicId()).isEqualTo(conceptOne.getPublicId());
        assertThat(result.concepts().get(1).slotIndex()).isEqualTo((short) 2);
        assertThat(result.concepts().get(1).conceptPublicId()).isEqualTo(conceptTwo.getPublicId());
        assertThat(result.weeklyFeaturedConceptPublicIds()).containsExactly(
                conceptTwo.getPublicId(),
                conceptOne.getPublicId()
        );
    }

    @Test
    void rejectsWhenConceptCountIsNotTwenty() {
        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(
                randomIds(19),
                randomIds(4)
        );

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenConceptsContainDuplicates() {
        List<UUID> conceptIds = randomIds(20);
        conceptIds.set(19, conceptIds.get(0));
        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(
                conceptIds,
                conceptIds.subList(0, 4)
        );

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("conceptPublicIds must contain unique values");
    }

    @Test
    void rejectsWhenWeeklyFeatureCountIsNotFour() {
        List<UUID> conceptIds = randomIds(20);
        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(
                conceptIds,
                conceptIds.subList(0, 3)
        );

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("weeklyFeaturedConceptPublicIds must contain exactly 4 items");
    }

    @Test
    void rejectsWhenWeeklyFeatureContainsConceptOutsidePack() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = new ArrayList<>(conceptIds.subList(0, 3));
        featuredIds.add(UUID.randomUUID());

        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(conceptIds, featuredIds);

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must be selected from conceptPublicIds");
    }

    @Test
    void rejectsWhenAtLeastOneConceptIsMissing() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = conceptIds.subList(0, 4);

        List<Concept> resolvedConcepts = conceptIds.stream()
                .limit(19)
                .map(this::conceptWithPublicId)
                .toList();
        when(conceptRepository.findAllByPublicIdIn(conceptIds)).thenReturn(resolvedConcepts);

        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(conceptIds, featuredIds);

        assertThatThrownBy(() -> service.upsertMonthlyPack("2026-04", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @SuppressWarnings("unchecked")
    void upsertReplacesRowsAndPreservesSlotOrdering() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = List.of(conceptIds.get(19), conceptIds.get(0), conceptIds.get(5), conceptIds.get(9));

        List<Concept> resolvedConcepts = conceptIds.stream()
                .map(this::conceptWithPublicId)
                .collect(java.util.stream.Collectors.toList());
        Collections.reverse(resolvedConcepts);
        when(conceptRepository.findAllByPublicIdIn(conceptIds)).thenReturn(resolvedConcepts);

        GameMonthlyPack existingPack = pack(300L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(existingPack));

        UpsertAdminGameMonthlyPackRequest request = new UpsertAdminGameMonthlyPackRequest(conceptIds, featuredIds);

        AdminGameMonthlyPackDto result = service.upsertMonthlyPack("2026-04", request);

        InOrder inOrder = inOrder(
                imposterMonthlyPackWeeklyFeatureRepository,
                imposterMonthlyPackConceptRepository,
                imposterMonthlyPackRepository
        );
        inOrder.verify(imposterMonthlyPackWeeklyFeatureRepository).deleteByPack_Id(300L);
        inOrder.verify(imposterMonthlyPackWeeklyFeatureRepository).flush();
        inOrder.verify(imposterMonthlyPackConceptRepository).deleteByPack_Id(300L);
        inOrder.verify(imposterMonthlyPackConceptRepository).flush();
        inOrder.verify(imposterMonthlyPackRepository).flush();

        ArgumentCaptor<List<GameMonthlyPackConcept>> conceptRowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(imposterMonthlyPackConceptRepository).saveAll(conceptRowsCaptor.capture());
        List<GameMonthlyPackConcept> savedConceptRows = conceptRowsCaptor.getValue();

        assertThat(savedConceptRows).hasSize(20);
        for (int i = 0; i < 20; i++) {
            assertThat(savedConceptRows.get(i).getSlotIndex()).isEqualTo((short) (i + 1));
            assertThat(savedConceptRows.get(i).getConcept().getPublicId()).isEqualTo(conceptIds.get(i));
        }

        ArgumentCaptor<List<GameMonthlyPackWeeklyFeature>> featuredRowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(imposterMonthlyPackWeeklyFeatureRepository).saveAll(featuredRowsCaptor.capture());
        List<GameMonthlyPackWeeklyFeature> savedFeatureRows = featuredRowsCaptor.getValue();

        assertThat(savedFeatureRows).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(savedFeatureRows.get(i).getWeekSlot()).isEqualTo((short) (i + 1));
            assertThat(savedFeatureRows.get(i).getConcept().getPublicId()).isEqualTo(featuredIds.get(i));
        }

        assertThat(result.exists()).isTrue();
        assertThat(result.yearMonth()).isEqualTo("2026-04");
        assertThat(result.concepts()).hasSize(20);
        assertThat(result.weeklyFeaturedConceptPublicIds()).containsExactlyElementsOf(featuredIds);
    }

    @Test
    void allowsResavingSameMonthWithoutSlotCollision() {
        List<UUID> firstConceptIds = randomIds(20);
        List<UUID> firstFeaturedIds = List.of(
                firstConceptIds.get(0),
                firstConceptIds.get(1),
                firstConceptIds.get(2),
                firstConceptIds.get(3)
        );
        List<UUID> secondConceptIds = randomIds(20);
        List<UUID> secondFeaturedIds = List.of(
                secondConceptIds.get(19),
                secondConceptIds.get(18),
                secondConceptIds.get(17),
                secondConceptIds.get(16)
        );

        when(conceptRepository.findAllByPublicIdIn(firstConceptIds)).thenReturn(firstConceptIds.stream()
                .map(this::conceptWithPublicId)
                .toList());
        when(conceptRepository.findAllByPublicIdIn(secondConceptIds)).thenReturn(secondConceptIds.stream()
                .map(this::conceptWithPublicId)
                .toList());

        GameMonthlyPack existingPack = pack(300L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(existingPack));

        AdminGameMonthlyPackDto firstSave = service.upsertMonthlyPack(
                "2026-04",
                new UpsertAdminGameMonthlyPackRequest(firstConceptIds, firstFeaturedIds)
        );
        AdminGameMonthlyPackDto secondSave = service.upsertMonthlyPack(
                "2026-04",
                new UpsertAdminGameMonthlyPackRequest(secondConceptIds, secondFeaturedIds)
        );

        assertThat(firstSave.weeklyFeaturedConceptPublicIds()).containsExactlyElementsOf(firstFeaturedIds);
        assertThat(secondSave.weeklyFeaturedConceptPublicIds()).containsExactlyElementsOf(secondFeaturedIds);
        verify(imposterMonthlyPackWeeklyFeatureRepository, times(2)).deleteByPack_Id(300L);
        verify(imposterMonthlyPackWeeklyFeatureRepository, times(2)).flush();
        verify(imposterMonthlyPackConceptRepository, times(2)).deleteByPack_Id(300L);
        verify(imposterMonthlyPackConceptRepository, times(2)).flush();
        verify(imposterMonthlyPackRepository, times(2)).flush();
    }

    @Test
    void derivesCurrentMonthFromClock() {
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.empty());

        AdminGameMonthlyPackDto result = service.getCurrentMonthlyPack();

        assertThat(result.yearMonth()).isEqualTo("2026-04");
    }

    @Test
    void upsertSyncsCurrentWeekFallbackAssignmentWhenNoSubmissions() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = List.of(conceptIds.get(5), conceptIds.get(6), conceptIds.get(7), conceptIds.get(8));

        List<Concept> resolvedConcepts = conceptIds.stream()
                .map(this::conceptWithPublicId)
                .toList();
        when(conceptRepository.findAllByPublicIdIn(conceptIds)).thenReturn(resolvedConcepts);

        GameMonthlyPack existingPack = pack(300L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(existingPack));

        WeeklyQuestWeek currentWeek = new WeeklyQuestWeek();
        ReflectionTestUtils.setField(currentWeek, "id", 77L);
        currentWeek.setStatus(WeeklyQuestWeekStatus.ACTIVE);
        when(weeklyQuestWeekRepository.findByWeekStartAt(OffsetDateTime.parse("2026-03-29T00:00:00+08:00")))
                .thenReturn(Optional.of(currentWeek));

        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        ReflectionTestUtils.setField(assignment, "id", 88L);
        assignment.setWeek(currentWeek);
        assignment.setSourceType(WeeklyQuestAssignmentSourceType.FALLBACK);
        assignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
        assignment.setConcept(conceptWithPublicId(conceptIds.get(0)));
        assignment.setCreatedAt(OffsetDateTime.parse("2026-03-29T00:00:00Z"));
        assignment.setUpdatedAt(OffsetDateTime.parse("2026-03-29T00:00:00Z"));
        when(weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(77L)).thenReturn(Optional.of(assignment));
        when(weeklyQuestChallengeSubmissionRepository.existsByWeeklyQuestAssignment_Id(88L)).thenReturn(false);

        service.upsertMonthlyPack("2026-04", new UpsertAdminGameMonthlyPackRequest(conceptIds, featuredIds));

        assertThat(assignment.getConcept().getPublicId()).isEqualTo(featuredIds.get(0));
        verify(weeklyQuestAssignmentRepository).save(assignment);
    }

    @Test
    void upsertDoesNotSyncWhenCurrentFallbackAssignmentHasSubmissions() {
        List<UUID> conceptIds = randomIds(20);
        List<UUID> featuredIds = List.of(conceptIds.get(5), conceptIds.get(6), conceptIds.get(7), conceptIds.get(8));

        List<Concept> resolvedConcepts = conceptIds.stream()
                .map(this::conceptWithPublicId)
                .toList();
        when(conceptRepository.findAllByPublicIdIn(conceptIds)).thenReturn(resolvedConcepts);

        GameMonthlyPack existingPack = pack(300L, "2026-04");
        when(imposterMonthlyPackRepository.findByYearMonth("2026-04")).thenReturn(Optional.of(existingPack));

        WeeklyQuestWeek currentWeek = new WeeklyQuestWeek();
        ReflectionTestUtils.setField(currentWeek, "id", 77L);
        currentWeek.setStatus(WeeklyQuestWeekStatus.ACTIVE);
        when(weeklyQuestWeekRepository.findByWeekStartAt(OffsetDateTime.parse("2026-03-29T00:00:00+08:00")))
                .thenReturn(Optional.of(currentWeek));

        WeeklyQuestAssignment assignment = new WeeklyQuestAssignment();
        ReflectionTestUtils.setField(assignment, "id", 88L);
        assignment.setWeek(currentWeek);
        assignment.setSourceType(WeeklyQuestAssignmentSourceType.FALLBACK);
        assignment.setStatus(WeeklyQuestAssignmentStatus.ACTIVE);
        assignment.setConcept(conceptWithPublicId(conceptIds.get(0)));
        assignment.setCreatedAt(OffsetDateTime.parse("2026-03-29T00:00:00Z"));
        assignment.setUpdatedAt(OffsetDateTime.parse("2026-03-29T00:00:00Z"));
        when(weeklyQuestAssignmentRepository.findByWeek_IdAndOfficialTrue(77L)).thenReturn(Optional.of(assignment));
        when(weeklyQuestChallengeSubmissionRepository.existsByWeeklyQuestAssignment_Id(88L)).thenReturn(true);

        service.upsertMonthlyPack("2026-04", new UpsertAdminGameMonthlyPackRequest(conceptIds, featuredIds));

        assertThat(assignment.getConcept().getPublicId()).isEqualTo(conceptIds.get(0));
        verify(weeklyQuestAssignmentRepository, times(0)).save(assignment);
    }

    private List<UUID> randomIds(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> UUID.randomUUID())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private GameMonthlyPack pack(Long id, String yearMonth) {
        GameMonthlyPack pack = new GameMonthlyPack();
        ReflectionTestUtils.setField(pack, "id", id);
        pack.setYearMonth(yearMonth);
        pack.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return pack;
    }

    private Concept concept(String title) {
        Concept concept = new Concept();
        ReflectionTestUtils.setField(concept, "publicId", UUID.randomUUID());
        concept.setTitle(title);
        concept.setDescription(title + " description");
        concept.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return concept;
    }

    private Concept conceptWithPublicId(UUID publicId) {
        Concept concept = concept("concept-" + publicId.toString().substring(0, 8));
        ReflectionTestUtils.setField(concept, "publicId", publicId);
        return concept;
    }

    private GameMonthlyPackConcept conceptRow(GameMonthlyPack pack, Concept concept, short slotIndex) {
        GameMonthlyPackConcept row = new GameMonthlyPackConcept();
        row.setPack(pack);
        row.setConcept(concept);
        row.setSlotIndex(slotIndex);
        return row;
    }

    private GameMonthlyPackWeeklyFeature weeklyFeature(GameMonthlyPack pack, Concept concept, short weekSlot) {
        GameMonthlyPackWeeklyFeature row = new GameMonthlyPackWeeklyFeature();
        row.setPack(pack);
        row.setConcept(concept);
        row.setWeekSlot(weekSlot);
        return row;
    }
}
