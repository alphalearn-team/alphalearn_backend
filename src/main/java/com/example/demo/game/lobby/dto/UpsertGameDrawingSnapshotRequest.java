package com.example.demo.game.lobby.dto;

public record UpsertGameDrawingSnapshotRequest(
        String snapshot,
        Integer baseVersion
) {
}
