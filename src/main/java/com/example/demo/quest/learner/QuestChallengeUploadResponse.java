package com.example.demo.quest.learner;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.example.demo.storage.r2.QuestChallengeStorageService;

public record QuestChallengeUploadResponse(
        UUID assignmentPublicId,
        String objectKey,
        String publicUrl,
        String uploadUrl,
        OffsetDateTime expiresAt,
        Map<String, String> requiredHeaders
) {
    public static QuestChallengeUploadResponse from(
            UUID assignmentPublicId,
            QuestChallengeStorageService.PresignedUpload upload
    ) {
        return new QuestChallengeUploadResponse(
                assignmentPublicId,
                upload.objectKey(),
                upload.publicUrl(),
                upload.uploadUrl(),
                upload.expiresAt(),
                upload.requiredHeaders()
        );
    }
}
