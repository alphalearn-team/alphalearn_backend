package com.example.demo.friendship.request.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.friendship.request.FriendRequestStatus;

public record FriendRequestDTO(
    Long requestId,
    UUID otherUserPublicId,
    String otherUsername,
    FriendRequestStatus status,
    OffsetDateTime createdAt
) {}