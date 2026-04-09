package com.example.demo.notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public record NotificationDto(
        UUID publicId,
        String message,
        String type,
        Map<String, Object> metadata,
        boolean isRead,
        OffsetDateTime createdAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getPublicId(),
                n.getMessage(),
                n.getType() == null ? NotificationType.GENERIC.name() : n.getType().name(),
                parseMetadata(n.getMetadataJson()),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private static Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(metadataJson, new TypeReference<>() {});
        } catch (Exception ex) {
            return null;
        }
    }
}
