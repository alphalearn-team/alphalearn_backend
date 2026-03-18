package com.example.demo.friendrequest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.friendrequest.FriendRequestStatus;

public record FriendRequestDTO(
    Long requestId,
    UUID otherUserPublicId,
    String otherUsername,
    FriendRequestStatus status,
    OffsetDateTime createdAt
) {}