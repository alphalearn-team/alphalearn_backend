package com.example.demo.friendship.request;

import java.util.Locale;

public enum FriendRequestDirection {
    INCOMING,
    OUTGOING;

    public static FriendRequestDirection fromQueryValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("direction is required");
        }
        return FriendRequestDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
