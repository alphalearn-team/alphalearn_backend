package com.example.demo.learner.profile;

import java.time.OffsetDateTime;
import java.util.Map;

import com.example.demo.storage.r2.ProfilePictureStorageService;

public record ProfilePictureUploadResponse(
        String objectKey,
        String publicUrl,
        String uploadUrl,
        OffsetDateTime expiresAt,
        Map<String, String> requiredHeaders
) {
    public static ProfilePictureUploadResponse from(ProfilePictureStorageService.PresignedUpload upload) {
        return new ProfilePictureUploadResponse(
                upload.objectKey(),
                upload.publicUrl(),
                upload.uploadUrl(),
                upload.expiresAt(),
                upload.requiredHeaders()
        );
    }
}
