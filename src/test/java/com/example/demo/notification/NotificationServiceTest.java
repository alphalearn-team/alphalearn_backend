package com.example.demo.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository);
    }

    @Test
    void createSavesNotification() {
        UUID learnerId = UUID.randomUUID();
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(learnerId, "Hello!");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getLearnerId()).isEqualTo(learnerId);
        assertThat(saved.getMessage()).isEqualTo("Hello!");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void createNoOpsWhenLearnerIdIsNull() {
        service.create(null, "Hello!");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getForLearnerReturnsNotificationsAsDtos() {
        UUID learnerId = UUID.randomUUID();
        Notification n = new Notification(learnerId, "Test");
        n.setPublicId(UUID.randomUUID());
        n.setCreatedAt(OffsetDateTime.now());
        when(notificationRepository.findByLearnerIdOrderByCreatedAtDesc(learnerId))
                .thenReturn(List.of(n));

        List<NotificationDto> result = service.getForLearner(learnerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Test");
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void markReadUpdatesNotificationForCorrectLearner() {
        UUID learnerId = UUID.randomUUID();
        UUID notifPublicId = UUID.randomUUID();
        Notification n = new Notification(learnerId, "Test");
        n.setPublicId(notifPublicId);
        n.setCreatedAt(OffsetDateTime.now());
        when(notificationRepository.findByPublicIdAndLearnerId(notifPublicId, learnerId))
                .thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markRead(learnerId, notifPublicId);

        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void markReadThrows404WhenNotificationNotFound() {
        UUID learnerId = UUID.randomUUID();
        UUID notifPublicId = UUID.randomUUID();
        when(notificationRepository.findByPublicIdAndLearnerId(notifPublicId, learnerId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.markRead(learnerId, notifPublicId));
    }
}
