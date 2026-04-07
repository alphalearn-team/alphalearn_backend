package com.example.demo.game.lobby.dto;

public record UpsertImposterDrawingSnapshotRequest(
        String snapshot,
        Integer baseVersion
) {
}
