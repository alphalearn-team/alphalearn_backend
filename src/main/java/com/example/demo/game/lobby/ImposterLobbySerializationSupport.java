package com.example.demo.game.lobby;

import com.example.demo.game.imposter.lobby.ImposterGameLobbyMember;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class ImposterLobbySerializationSupport {

    String serializeDrawerOrder(List<UUID> drawerOrder) {
        return serializeUuidList(drawerOrder);
    }

    List<UUID> deserializeDrawerOrder(String serializedOrder) {
        return deserializeUuidList(serializedOrder);
    }

    String serializeUuidList(List<UUID> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    List<UUID> deserializeUuidList(String serializedValues) {
        if (serializedValues == null || serializedValues.isBlank()) {
            return List.of();
        }

        return Arrays.stream(serializedValues.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(UUID::fromString)
                .toList();
    }

    String serializeVoteBallots(Map<UUID, UUID> ballots) {
        if (ballots == null || ballots.isEmpty()) {
            return null;
        }

        return ballots.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    Map<UUID, UUID> deserializeVoteBallots(String serializedBallots) {
        if (serializedBallots == null || serializedBallots.isBlank()) {
            return new HashMap<>();
        }

        Map<UUID, UUID> ballots = new HashMap<>();
        for (String token : serializedBallots.split(",")) {
            String value = token.trim();
            if (value.isBlank()) {
                continue;
            }
            String[] pair = value.split(":");
            if (pair.length != 2) {
                continue;
            }
            ballots.put(UUID.fromString(pair[0].trim()), UUID.fromString(pair[1].trim()));
        }
        return ballots;
    }

    Map<UUID, Integer> buildVoteTallyFromBallots(String serializedBallots, List<UUID> eligibleTargets) {
        Map<UUID, Integer> tallies = new HashMap<>();
        if (eligibleTargets != null) {
            for (UUID eligibleTarget : eligibleTargets) {
                tallies.put(eligibleTarget, 0);
            }
        }
        Map<UUID, UUID> ballots = deserializeVoteBallots(serializedBallots);
        for (UUID target : ballots.values()) {
            if (tallies.containsKey(target)) {
                tallies.put(target, tallies.get(target) + 1);
            }
        }
        return tallies;
    }

    String serializeVoteTallies(Map<UUID, Integer> tallies) {
        if (tallies == null || tallies.isEmpty()) {
            return null;
        }
        return tallies.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    Map<UUID, Integer> deserializeVoteTallies(String serializedTallies) {
        if (serializedTallies == null || serializedTallies.isBlank()) {
            return new HashMap<>();
        }

        Map<UUID, Integer> tallies = new HashMap<>();
        for (String token : serializedTallies.split(",")) {
            String value = token.trim();
            if (value.isBlank()) {
                continue;
            }
            String[] pair = value.split(":");
            if (pair.length != 2) {
                continue;
            }
            tallies.put(UUID.fromString(pair[0].trim()), Integer.parseInt(pair[1].trim()));
        }
        return tallies;
    }

    String serializeScoreMap(Map<UUID, Integer> scoreMap) {
        return serializeVoteTallies(scoreMap);
    }

    Map<UUID, Integer> deserializeScoreMap(String serializedScores) {
        return deserializeVoteTallies(serializedScores);
    }

    Map<UUID, Integer> initializeScoreMap(List<ImposterGameLobbyMember> activeMembers) {
        Map<UUID, Integer> scoreMap = new HashMap<>();
        for (ImposterGameLobbyMember activeMember : activeMembers) {
            scoreMap.put(activeMember.getLearnerId(), 0);
        }
        return scoreMap;
    }
}
