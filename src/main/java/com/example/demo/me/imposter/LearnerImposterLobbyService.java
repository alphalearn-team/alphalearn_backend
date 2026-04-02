package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyMemberRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyCodeGenerator;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinPrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.JoinedPrivateImposterLobbyDto;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerImposterLobbyService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int LOBBY_CODE_MAX_RETRIES = 10;
    private static final String LOBBY_CODE_UNIQUE_CONSTRAINT = "uk_imposter_game_lobbies_lobby_code";
    private static final String MEMBER_UNIQUE_CONSTRAINT = "uk_imposter_game_lobby_members_lobby_learner";

    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository;
    private final ImposterLobbyCodeGenerator imposterLobbyCodeGenerator;
    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final Clock clock;

    public LearnerImposterLobbyService(
            ImposterGameLobbyRepository imposterGameLobbyRepository,
            ImposterGameLobbyMemberRepository imposterGameLobbyMemberRepository,
            ImposterLobbyCodeGenerator imposterLobbyCodeGenerator,
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            Clock clock
    ) {
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterGameLobbyMemberRepository = imposterGameLobbyMemberRepository;
        this.imposterLobbyCodeGenerator = imposterLobbyCodeGenerator;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
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

        ImposterGameLobbyMember existingMember = imposterGameLobbyMemberRepository
                .findByLobby_IdAndLearnerId(lobby.getId(), user.userId())
                .orElse(null);
        if (existingMember != null) {
            return JoinedPrivateImposterLobbyDto.from(lobby, existingMember, true);
        }

        ImposterGameLobbyMember createdMember;
        try {
            createdMember = createMembership(lobby, user.userId());
        } catch (DataIntegrityViolationException ex) {
            if (!isMemberUniqueViolation(ex)) {
                throw ex;
            }
            createdMember = imposterGameLobbyMemberRepository.findByLobby_IdAndLearnerId(lobby.getId(), user.userId())
                    .orElseThrow(() -> ex);
            return JoinedPrivateImposterLobbyDto.from(lobby, createdMember, true);
        }

        return JoinedPrivateImposterLobbyDto.from(lobby, createdMember, false);
    }

    private ImposterGameLobbyMember createMembership(ImposterGameLobby lobby, java.util.UUID learnerId) {
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
