package com.example.demo.me.imposter.dto;

public record UpdatePrivateImposterLobbySettingsRequest(
        Integer conceptCount,
        Integer roundsPerConcept,
        Integer discussionTimerSeconds,
        Integer imposterGuessTimerSeconds,
        Integer turnDurationSeconds
) {
}
