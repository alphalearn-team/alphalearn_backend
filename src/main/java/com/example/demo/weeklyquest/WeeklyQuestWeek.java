package com.example.demo.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.weeklyquest.enums.WeeklyQuestActivationSource;
import com.example.demo.weeklyquest.enums.WeeklyQuestWeekStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weekly_quest_weeks")
public class WeeklyQuestWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    @Setter(lombok.AccessLevel.NONE)
    private UUID publicId;

    @Column(name = "week_start_at", nullable = false, unique = true)
    private OffsetDateTime weekStartAt;

    @Column(name = "setup_deadline_at", nullable = false)
    private OffsetDateTime setupDeadlineAt;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "weekly_quest_week_status")
    private WeeklyQuestWeekStatus status;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "activation_source", nullable = false, columnDefinition = "weekly_quest_activation_source")
    private WeeklyQuestActivationSource activationSource;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
