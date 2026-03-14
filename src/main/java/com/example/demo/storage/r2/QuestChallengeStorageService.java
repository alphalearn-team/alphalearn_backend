package com.example.demo.storage.r2;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class QuestChallengeStorageService {

    private final R2StorageProperties properties;
    private final QuestChallengeObjectKeyFactory objectKeyFactory;
    private final ObjectProvider<S3Presigner> presignerProvider;

    public QuestChallengeStorageService(
            R2StorageProperties properties,
            QuestChallengeObjectKeyFactory objectKeyFactory,
            ObjectProvider<S3Presigner> presignerProvider
    ) {
        this.properties = properties;
        this.objectKeyFactory = objectKeyFactory;
        this.presignerProvider = presignerProvider;
    }

    public String buildObjectKey(UUID assignmentPublicId, UUID learnerId, String originalFilename) {
        return objectKeyFactory.buildObjectKey(assignmentPublicId, learnerId, originalFilename);
    }

    public String buildPublicUrl(String objectKey) {
        if (properties.normalizedPublicBaseUrl().isBlank()) {
            return null;
        }
        return properties.normalizedPublicBaseUrl() + "/" + objectKey;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public void requireEnabled() {
        if (!properties.enabled() || presignerProvider.getIfAvailable() == null) {
            throw new IllegalStateException("Quest challenge storage is not enabled");
        }
    }

    public PresignedUpload generatePresignedUpload(
            UUID assignmentPublicId,
            UUID learnerId,
            String originalFilename,
            String contentType
    ) {
        requireEnabled();

        String objectKey = buildObjectKey(assignmentPublicId, learnerId, originalFilename);
        Duration expiration = Duration.ofMinutes(properties.presignExpiryMinutes());
        S3Presigner presigner = presignerProvider.getObject();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PresignedUpload(
                objectKey,
                buildPublicUrl(objectKey),
                presignedRequest.url().toString(),
                OffsetDateTime.now(ZoneOffset.UTC).plus(expiration),
                Map.of("Content-Type", contentType)
        );
    }

    public long maxUploadSizeBytes() {
        return properties.maxUploadSizeBytes();
    }

    public record PresignedUpload(
            String objectKey,
            String publicUrl,
            String uploadUrl,
            OffsetDateTime expiresAt,
            Map<String, String> requiredHeaders
    ) {}
}
