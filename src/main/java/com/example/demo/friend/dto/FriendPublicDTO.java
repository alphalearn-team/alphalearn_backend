package com.example.demo.friend.dto;

import java.util.UUID;

public record FriendPublicDTO(
        UUID publicId,
        String username
) {}