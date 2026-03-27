package com.example.demo.friendship.shared;

import java.util.UUID;

public record OrderedUuidPair(
        UUID first,
        UUID second
) {
    public static OrderedUuidPair of(UUID left, UUID right) {
        if (left.toString().compareTo(right.toString()) <= 0) {
            return new OrderedUuidPair(left, right);
        }
        return new OrderedUuidPair(right, left);
    }
}
