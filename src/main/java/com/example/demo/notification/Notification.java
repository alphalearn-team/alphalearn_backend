package com.example.demo.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "learner_id", columnDefinition = "uuid", nullable = false)
    private UUID learnerId;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type = NotificationType.GENERIC;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Notification(UUID learnerId, String message) {
        this(learnerId, message, NotificationType.GENERIC, null);
    }

    public Notification(UUID learnerId, String message, NotificationType type, String metadataJson) {
        this.learnerId = learnerId;
        this.message = message;
        this.type = type == null ? NotificationType.GENERIC : type;
        this.metadataJson = metadataJson;
        this.isRead = false;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
