package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyCodeGenerator;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyMemberStateDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyStateDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerImposterLobbyService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int LOBBY_CODE_MAX_RETRIES = 10;
    private static final int MIN_ACTIVE_MEMBERS_TO_START = 3;
    private static final String LOBBY_CODE_UNIQUE_CONSTRAINT = "uk_imposter_game_lobbies_lobby_code";
    private static final String MEMBER_UNIQUE_CONSTRAINT = "uk_imposter_game_lobby_members_lobby_learner";

    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final ImposterLobbyCodeGenerator imposterLobbyCodeGenerator;
    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final LearnerRepository learnerRepository;
    private final Clock clock;

    public LearnerImposterLobbyService(
            ImposterGameLobbyRepository imposterGameLobbyRepository,
            ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository,
            ImposterLobbyCodeGenerator imposterLobbyCodeGenerator,
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            LearnerRepository learnerRepository,
            Clock clock
    ) {
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.imposterLobbyCodeGenerator = imposterLobbyCodeGenerator;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.learnerRepository = learnerRepository;
        this.clock = clock;
    }

    @Transactional
    public PrivateImposterLobbyDto createPrivateLobby(
            SupabaseAuthUser user,
            CreatePrivateImposterLobbyRequest request
    ) {
        requireLearner(user);
        if (request == null || request.conceptPoolMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPoolMode is required");
        }

        String pinnedYearMonth = null;
        if (request.conceptPoolMode() == ImposterLobbyConceptPoolMode.CURRENT_MONTH_PACK) {
            String currentYearMonth = YearMonth.now(clock).format(YEAR_MONTH_FORMATTER);
            ImposterMonthlyPack pack = imposterMonthlyPackRepository.findByYearMonth(currentYearMonth)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Current monthly imposter pack is not configured"
                    ));

            if (imposterMonthlyPackConceptRepository.findByPack_IdOrderBySlotIndexAsc(pack.getId()).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Current monthly imposter pack has no concepts"
                );
            }

            pinnedYearMonth = currentYearMonth;
        }

        for (int attempt = 1; attempt <= LOBBY_CODE_MAX_RETRIES; attempt++) {
            ImposterGameLobby lobby = new ImposterGameLobby();
            lobby.setLobbyCode(imposterLobbyCodeGenerator.generate());
            lobby.setHostLearnerId(user.userId());
            lobby.setPrivateLobby(true);
            lobby.setConceptPoolMode(request.conceptPoolMode());
            lobby.setPinnedYearMonth(pinnedYearMonth);
            lobby.setCreatedAt(OffsetDateTime.now(clock));

            try {
                ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
                createMembership(savedLobby, user.userId());
                return PrivateImposterLobbyDto.from(savedLobby);
            } catch (DataIntegrityViolationException ex) {
                if (!isLobbyCodeUniqueViolation(ex) || attempt == LOBBY_CODE_MAX_RETRIES) {
                    throw ex;
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate lobby code");
    }

    @Transactional
    public JoinedPrivateImposterLobbyDto joinPrivateLobby(SupabaseAuthUser user, JoinPrivateImposterLobbyRequest request) {
        requireLearner(user);
        String lobbyCode = normalizeLobbyCode(request == null ? null : request.lobbyCode());
        if (lobbyCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyCode is required");
        }

        ImposterGameLobby lobby = imposterGameLobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));

        ImposterGameLobbyMember activeMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .orElse(null);
        if (activeMember != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Learner already joined this lobby");
        }

        ImposterGameLobbyMember historicalMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerId(lobby.getId(), user.userId())
                .orElse(null);
        if (historicalMember != null) {
            historicalMember.setJoinedAt(OffsetDateTime.now(clock));
            historicalMember.setLeftAt(null);
            ImposterGameLobbyMember rejoinedMember = imposterGameLobbyMemberRepository.saveAndFlush(historicalMember);
            return JoinedPrivateImposterLobbyDto.from(lobby, rejoinedMember, false);
        }

        try {
            ImposterGameLobbyMember createdMember = createMembership(lobby, user.userId());
            return JoinedPrivateImposterLobbyDto.from(lobby, createdMember, false);
        } catch (DataIntegrityViolationException ex) {
            if (isMemberUniqueViolation(ex)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Learner already joined this lobby");
            }
            throw ex;
        }
    }

    @Transactional
    public PrivateImposterLobbyStateDto leavePrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId);
        ensureViewerIsMember(lobby, user.userId());

        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby has already started");
        }

        ImposterGameLobbyMember member = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Learner is not an active member of this lobby"
                ));

        member.setLeftAt(OffsetDateTime.now(clock));
        imposterGameLobbyMemberRepository.saveAndFlush(member);

        return buildLobbyState(lobby, user.userId());
    }

    @Transactional
    public PrivateImposterLobbyStateDto startPrivateLobby(SupabaseAuthUser user, UUID lobbyPublicId) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId);
        ensureViewerIsMember(lobby, user.userId());

        if (!user.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can start this imposter lobby");
        }

        if (lobby.getStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imposter lobby has already started");
        }

        boolean hostIsActiveMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), user.userId())
                .isPresent();
        if (!hostIsActiveMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby host must be an active member to start");
        }

        long activeMemberCount = imposterGameLobbyMemberRepository.countByLobby_IdAndLeftAtIsNull(lobby.getId());
        if (activeMemberCount < MIN_ACTIVE_MEMBERS_TO_START) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "At least " + MIN_ACTIVE_MEMBERS_TO_START + " active players are required to start"
            );
        }

        lobby.setStartedAt(OffsetDateTime.now(clock));
        lobby.setStartedByLearnerId(user.userId());
        ImposterGameLobby savedLobby = imposterGameLobbyRepository.saveAndFlush(lobby);
        return buildLobbyState(savedLobby, user.userId());
    }

    @Transactional(readOnly = true)
    public PrivateImposterLobbyStateDto getPrivateLobbyState(SupabaseAuthUser user, UUID lobbyPublicId) {
        requireLearner(user);
        ImposterGameLobby lobby = resolveLobbyByPublicId(lobbyPublicId);
        ensureViewerIsMember(lobby, user.userId());
        return buildLobbyState(lobby, user.userId());
    }

    private ImposterGameLobby resolveLobbyByPublicId(UUID lobbyPublicId) {
        if (lobbyPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyPublicId is required");
        }

        return imposterGameLobbyRepository.findByPublicId(lobbyPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imposter lobby not found"));
    }

    private void ensureViewerIsMember(ImposterGameLobby lobby, UUID learnerId) {
        if (!imposterGameLobbyMemberRepository.existsByLobby_IdAndLearnerId(lobby.getId(), learnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner is not a member of this lobby");
        }
    }

    private PrivateImposterLobbyStateDto buildLobbyState(ImposterGameLobby lobby, UUID viewerLearnerId) {
        List<ImposterGameLobbyMember> activeMembers = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLeftAtIsNullOrderByJoinedAtAsc(lobby.getId());

        Map<UUID, Learner> learnersById = learnerRepository.findAllById(
                        activeMembers.stream().map(ImposterGameLobbyMember::getLearnerId).toList()
                )
                .stream()
                .collect(Collectors.toMap(Learner::getId, Function.identity()));

        List<PrivateImposterLobbyMemberStateDto> activeMemberDtos = activeMembers.stream()
                .map(member -> {
                    Learner learner = learnersById.get(member.getLearnerId());
                    return new PrivateImposterLobbyMemberStateDto(
                            learner == null ? null : learner.getPublicId(),
                            learner == null ? null : learner.getUsername(),
                            member.getJoinedAt(),
                            member.getLearnerId().equals(lobby.getHostLearnerId())
                    );
                })
                .toList();

        long activeMemberCount = activeMemberDtos.size();
        boolean viewerIsHost = viewerLearnerId.equals(lobby.getHostLearnerId());
        boolean viewerIsActiveMember = activeMembers.stream()
                .anyMatch(member -> member.getLearnerId().equals(viewerLearnerId));
        boolean notStarted = lobby.getStartedAt() == null;

        return new PrivateImposterLobbyStateDto(
                lobby.getPublicId(),
                lobby.getLobbyCode(),
                lobby.isPrivateLobby(),
                lobby.getConceptPoolMode(),
                lobby.getPinnedYearMonth(),
                lobby.getCreatedAt(),
                lobby.getStartedAt(),
                activeMemberCount,
                activeMemberDtos,
                viewerIsHost,
                viewerIsActiveMember,
                viewerIsActiveMember && notStarted,
                viewerIsHost && viewerIsActiveMember && notStarted && activeMemberCount >= MIN_ACTIVE_MEMBERS_TO_START
        );
    }

    private ImposterGameLobbyMember createMembership(ImposterGameLobby lobby, UUID learnerId) {
        ImposterGameLobbyMember member = new ImposterGameLobbyMember();
        member.setLobby(lobby);
        member.setLearnerId(learnerId);
        member.setJoinedAt(OffsetDateTime.now(clock));
        return imposterGameLobbyMemberRepository.saveAndFlush(member);
    }

    private SupabaseAuthUser requireLearner(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user;
    }

    private String normalizeLobbyCode(String lobbyCode) {
        if (lobbyCode == null) {
            return null;
        }

        String normalized = lobbyCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private boolean isLobbyCodeUniqueViolation(Throwable error) {
        return hasConstraintViolation(error, LOBBY_CODE_UNIQUE_CONSTRAINT);
    }

    private boolean isMemberUniqueViolation(Throwable error) {
        return hasConstraintViolation(error, MEMBER_UNIQUE_CONSTRAINT);
    }

    private boolean hasConstraintViolation(Throwable error, String constraintName) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConstraintViolationException violationException) {
                return constraintName.equalsIgnoreCase(violationException.getConstraintName());
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
