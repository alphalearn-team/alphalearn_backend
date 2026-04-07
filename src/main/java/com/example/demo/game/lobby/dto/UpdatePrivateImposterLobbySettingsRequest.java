package com.example.demo.game.lobby.dto;

public record UpdatePrivateImposterLobbySettingsRequest(
        Integer conceptCount,
        Integer roundsPerConcept,
        Integer discussionTimerSeconds,
        Integer imposterGuessTimerSeconds,
        Integer turnDurationSeconds
) {
}
