package com.example.demo.storage.r2;

import java.util.UUID;

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
}
