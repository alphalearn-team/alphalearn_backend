package com.example.demo.friendship.request.dto;

import com.example.demo.friendship.request.FriendRequestStatus;

public record UpdateFriendRequestStatusRequest(
        FriendRequestStatus status
) {
}
