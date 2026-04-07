package com.example.demo.me.imposter.dto;

import java.util.List;
import java.util.UUID;

public record PrivateImposterLobbyConceptResultDto(
        Integer conceptNumber,
        String conceptLabel,
        ImposterConceptWinnerSide winnerSide,
        ImposterConceptResolution resolution,
        UUID accusedPublicId,
        UUID imposterPublicId,
        boolean imposterWinsByVotingTie,
        String imposterGuess,
        List<PrivateImposterLobbyVoteTallyDto> finalVoteTallies
) {
}
