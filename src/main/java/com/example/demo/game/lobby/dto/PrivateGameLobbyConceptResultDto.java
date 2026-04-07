package com.example.demo.game.lobby.dto;

import java.util.List;
import java.util.UUID;

public record PrivateGameLobbyConceptResultDto(
        Integer conceptNumber,
        String conceptLabel,
        GameConceptWinnerSide winnerSide,
        GameConceptResolution resolution,
        UUID accusedPublicId,
        UUID imposterPublicId,
        boolean imposterWinsByVotingTie,
        String imposterGuess,
        List<PrivateGameLobbyVoteTallyDto> finalVoteTallies
) {
}
