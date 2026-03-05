package com.example.demo.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID publicId,
        String message,
        boolean isRead,
        OffsetDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getPublicId(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
