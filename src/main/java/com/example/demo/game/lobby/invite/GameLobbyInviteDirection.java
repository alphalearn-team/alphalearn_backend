package com.example.demo.game.lobby.invite;

public enum GameLobbyInviteDirection {
    INCOMING,
    OUTGOING;

    public static GameLobbyInviteDirection fromQueryValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("direction is required");
        }
        return switch (raw.trim().toUpperCase()) {
            case "INCOMING" -> INCOMING;
            case "OUTGOING" -> OUTGOING;
            default -> throw new IllegalArgumentException("direction must be INCOMING or OUTGOING");
        };
    }
}
