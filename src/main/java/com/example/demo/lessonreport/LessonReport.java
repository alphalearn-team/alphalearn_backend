package com.example.demo.lessonreport;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.lesson.Lesson;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lesson_reports")
public class LessonReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_report_id")
    @Setter(lombok.AccessLevel.NONE)
    private Long lessonReportId;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "reporter_user_id", columnDefinition = "uuid", nullable = false)
    private UUID reporterUserId;

    @Column(name = "reason", columnDefinition = "text", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "lesson_report_status")
    private LessonReportStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by_admin_user_id", columnDefinition = "uuid")
    private UUID resolvedByAdminUserId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "resolution_action", columnDefinition = "lesson_report_resolution_action")
    private LessonReportResolutionAction resolutionAction;

    @PrePersist
    void assignDefaultsIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = LessonReportStatus.PENDING;
        }
    }
}
