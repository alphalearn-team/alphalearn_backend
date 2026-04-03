package com.example.demo.me.imposter.dto;

public record UpsertImposterDrawingSnapshotRequest(
        String snapshot,
        Integer baseVersion
) {
}
