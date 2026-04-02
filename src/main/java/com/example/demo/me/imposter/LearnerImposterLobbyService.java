package com.example.demo.me.imposter;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.game.imposter.lobby.ImposterGameLobby;
import com.example.demo.game.imposter.lobby.ImposterGameLobbyRepository;
import com.example.demo.game.imposter.lobby.ImposterLobbyConceptPoolMode;
import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackConceptRepository;
import com.example.demo.game.imposter.monthly.repository.ImposterMonthlyPackRepository;
import com.example.demo.me.imposter.dto.CreatePrivateImposterLobbyRequest;
import com.example.demo.me.imposter.dto.PrivateImposterLobbyDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearnerImposterLobbyService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ImposterGameLobbyRepository imposterGameLobbyRepository;
    private final ImposterMonthlyPackRepository imposterMonthlyPackRepository;
    private final ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository;
    private final Clock clock;

    public LearnerImposterLobbyService(
            ImposterGameLobbyRepository imposterGameLobbyRepository,
            ImposterMonthlyPackRepository imposterMonthlyPackRepository,
            ImposterMonthlyPackConceptRepository imposterMonthlyPackConceptRepository,
            Clock clock
    ) {
        this.imposterGameLobbyRepository = imposterGameLobbyRepository;
        this.imposterMonthlyPackRepository = imposterMonthlyPackRepository;
        this.imposterMonthlyPackConceptRepository = imposterMonthlyPackConceptRepository;
        this.clock = clock;
    }

    @Transactional
    public PrivateImposterLobbyDto createPrivateLobby(
            SupabaseAuthUser user,
            CreatePrivateImposterLobbyRequest request
    ) {
        if (user == null || user.userId() == null || user.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        if (request == null || request.conceptPoolMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conceptPoolMode is required");
        }

        ImposterGameLobby lobby = new ImposterGameLobby();
        lobby.setHostLearnerId(user.userId());
        lobby.setPrivateLobby(true);
        lobby.setConceptPoolMode(request.conceptPoolMode());
        lobby.setCreatedAt(OffsetDateTime.now(clock));

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

            lobby.setPinnedYearMonth(currentYearMonth);
        } else {
            lobby.setPinnedYearMonth(null);
        }

        return PrivateImposterLobbyDto.from(imposterGameLobbyRepository.save(lobby));
    }
}
