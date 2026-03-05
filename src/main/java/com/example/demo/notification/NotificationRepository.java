package com.example.demo.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByLearnerIdOrderByCreatedAtDesc(UUID learnerId);

    Optional<Notification> findByPublicIdAndLearnerId(UUID publicId, UUID learnerId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.learnerId = :learnerId AND n.isRead = false")
    void markAllReadByLearnerId(@Param("learnerId") UUID learnerId);
}
