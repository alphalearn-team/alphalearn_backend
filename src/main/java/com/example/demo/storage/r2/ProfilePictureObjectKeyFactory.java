package com.example.demo.storage.r2;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ProfilePictureObjectKeyFactory {

    public String buildObjectKey(UUID learnerId, String originalFilename) {
        return "profile-pictures/%s/%s-%s".formatted(
                learnerId,
                UUID.randomUUID(),
                sanitizeFilename(originalFilename)
        );
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }

        String sanitized = originalFilename.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");

        if (sanitized.isBlank() || ".".equals(sanitized) || "..".equals(sanitized)) {
            return "upload.bin";
        }

        return sanitized;
    }
}
