package com.example.demo.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.concept.Concept;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentSourceType;
import com.example.demo.weeklyquest.enums.WeeklyQuestAssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "weekly_quest_assignments")
public class WeeklyQuestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    @Setter(lombok.AccessLevel.NONE)
    private UUID publicId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "week_id", nullable = false)
    private WeeklyQuestWeek week;

    @ManyToOne(optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(name = "slot_index", nullable = false)
    private short slotIndex = 0;

    @Column(name = "is_official", nullable = false)
    private boolean official = true;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "weekly_quest_assignment_source_type")
    private WeeklyQuestAssignmentSourceType sourceType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "weekly_quest_assignment_status")
    private WeeklyQuestAssignmentStatus status;

    @Column(name = "created_by_admin_id", columnDefinition = "uuid")
    private UUID createdByAdminId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
