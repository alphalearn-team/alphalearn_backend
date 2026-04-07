package com.example.demo.game.lobby.dto;

import java.util.UUID;

public record SubmitImposterVoteRequest(
        UUID suspectedLearnerPublicId
) {
}
