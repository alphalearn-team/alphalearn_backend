package com.example.demo.game.lobby.dto;

public record UpdatePrivateGameLobbySettingsRequest(
        Integer conceptCount,
        Integer roundsPerConcept,
        Integer discussionTimerSeconds,
        Integer imposterGuessTimerSeconds,
        Integer turnDurationSeconds
) {
}
