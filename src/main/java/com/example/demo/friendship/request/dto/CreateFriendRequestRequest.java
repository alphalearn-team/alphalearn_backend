package com.example.demo.friendship.request.dto;

import java.util.UUID;

public record CreateFriendRequestRequest(
        UUID receiverPublicId
) {
}
