package com.example.demo.friendship.friend.dto;

import java.util.UUID;

public record FriendPublicDTO(
        UUID publicId,
        String username,
        String profilePictureUrl
) {}
