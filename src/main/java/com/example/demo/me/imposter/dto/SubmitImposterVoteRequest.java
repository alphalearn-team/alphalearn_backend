package com.example.demo.me.imposter.dto;

import java.util.UUID;

public record SubmitImposterVoteRequest(
        UUID suspectedLearnerPublicId
) {
}
