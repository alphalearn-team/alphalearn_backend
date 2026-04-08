package com.example.demo.notification.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NotificationActionRequest", description = "Payload for notification actions")
public record NotificationActionRequest(
        @Schema(description = "Supported values: READ, READ_ALL")
        String action
) {}
