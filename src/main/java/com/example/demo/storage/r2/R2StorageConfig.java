package com.example.demo.storage.r2;

import java.util.Locale;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "storage.r2", name = "enabled", havingValue = "true")
    public S3Client r2S3Client(R2StorageProperties properties) {
        validate(properties);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKey(),
                properties.secretKey()
        );

        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage.r2", name = "enabled", havingValue = "true")
    public S3Presigner r2S3Presigner(R2StorageProperties properties) {
        validate(properties);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKey(),
                properties.secretKey()
        );

        return S3Presigner.builder()
                .endpointOverride(properties.endpoint())
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private void validate(R2StorageProperties properties) {
        if (!properties.isFullyConfigured()) {
            throw new IllegalStateException("""
                    storage.r2 is enabled but incomplete.
                    Required properties: storage.r2.account-id, storage.r2.access-key, storage.r2.secret-key,
                    storage.r2.bucket, storage.r2.public-base-url
                    """.stripIndent().trim());
        }
        if (!properties.normalizedPublicBaseUrl().toLowerCase(Locale.ROOT).startsWith("http")) {
            throw new IllegalStateException("storage.r2.public-base-url must start with http or https");
        }
    }
}
