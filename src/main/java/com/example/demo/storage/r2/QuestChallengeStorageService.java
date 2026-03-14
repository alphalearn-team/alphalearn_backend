package com.example.demo.storage.r2;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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
    private final ObjectProvider<S3Client> s3ClientProvider;

    public QuestChallengeStorageService(
            R2StorageProperties properties,
            QuestChallengeObjectKeyFactory objectKeyFactory,
            ObjectProvider<S3Presigner> presignerProvider,
            ObjectProvider<S3Client> s3ClientProvider
    ) {
        this.properties = properties;
        this.objectKeyFactory = objectKeyFactory;
        this.presignerProvider = presignerProvider;
        this.s3ClientProvider = s3ClientProvider;
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
        if (!properties.enabled() || presignerProvider.getIfAvailable() == null || s3ClientProvider.getIfAvailable() == null) {
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

    public String expectedObjectKeyPrefix(UUID assignmentPublicId, UUID learnerId) {
        return "quest-challenges/%s/%s/".formatted(assignmentPublicId, learnerId);
    }

    public StoredObjectMetadata fetchObjectMetadata(String objectKey) {
        requireEnabled();

        try {
            S3Client s3Client = s3ClientProvider.getObject();
            var response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
            return new StoredObjectMetadata(
                    response.contentType(),
                    response.contentLength(),
                    buildPublicUrl(objectKey)
            );
        } catch (NoSuchKeyException ex) {
            throw new IllegalArgumentException("Uploaded object was not found", ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new IllegalArgumentException("Uploaded object was not found", ex);
            }
            throw ex;
        }
    }

    public record PresignedUpload(
            String objectKey,
            String publicUrl,
            String uploadUrl,
            OffsetDateTime expiresAt,
            Map<String, String> requiredHeaders
    ) {}

    public record StoredObjectMetadata(
            String contentType,
            long fileSizeBytes,
            String publicUrl
    ) {}
}
