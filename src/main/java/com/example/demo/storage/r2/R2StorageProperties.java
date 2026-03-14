package com.example.demo.storage.r2;

import java.net.URI;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.r2")
public record R2StorageProperties(
        boolean enabled,
        String accountId,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBaseUrl,
        @Min(1) int presignExpiryMinutes,
        @Min(1) long maxUploadSizeBytes
) {
    public URI endpoint() {
        return URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
    }

    public boolean isFullyConfigured() {
        return hasText(accountId)
                && hasText(accessKey)
                && hasText(secretKey)
                && hasText(bucket)
                && hasText(publicBaseUrl);
    }

    public String normalizedPublicBaseUrl() {
        if (!hasText(publicBaseUrl)) {
            return "";
        }
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
