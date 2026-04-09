package com.example.demo.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a notification using the learner's internal UUID (learners.id / PK).
     * Call this when you already have the Learner entity — no extra DB lookup needed.
     */
    @Transactional
    public void create(UUID learnerInternalId, String message) {
        create(learnerInternalId, message, NotificationType.GENERIC, null);
    }

    @Transactional
    public void create(UUID learnerInternalId, String message, NotificationType type, Map<String, Object> metadata) {
        if (learnerInternalId == null || message == null) return;
        notificationRepository.save(new Notification(
                learnerInternalId,
                message,
                type == null ? NotificationType.GENERIC : type,
                serializeMetadata(metadata)
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getForLearner(UUID learnerInternalId) {
        return notificationRepository
                .findByLearnerIdOrderByCreatedAtDesc(learnerInternalId)
                .stream()
                .map(NotificationDto::from)
                .toList();
    }

    @Transactional
    public void markRead(UUID learnerInternalId, UUID notificationPublicId) {
        Notification notification = notificationRepository
                .findByPublicIdAndLearnerId(notificationPublicId, learnerInternalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(UUID learnerInternalId) {
        notificationRepository.markAllReadByLearnerId(learnerInternalId);
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize notification metadata", ex);
        }
    }
}
