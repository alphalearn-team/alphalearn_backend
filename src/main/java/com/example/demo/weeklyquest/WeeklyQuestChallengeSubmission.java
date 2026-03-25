package com.example.demo.weeklyquest;

import com.example.demo.learner.Learner;
import com.example.demo.weeklyquest.enums.SubmissionVisibility;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weekly_quest_challenge_submissions")
public class WeeklyQuestChallengeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    @Setter(lombok.AccessLevel.NONE)
    private UUID publicId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "weekly_quest_assignment_id", nullable = false)
    private WeeklyQuestAssignment weeklyQuestAssignment;

    @Column(name = "media_object_key", nullable = false, columnDefinition = "text")
    private String mediaObjectKey;

    @Column(name = "media_public_url", columnDefinition = "text")
    private String mediaPublicUrl;

    @Column(name = "media_content_type", nullable = false)
    private String mediaContentType;

    @Column(name = "original_filename", nullable = false, columnDefinition = "text")
    private String originalFilename;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "caption", columnDefinition = "text")
    private String caption;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "quest_submission_visibility")
    private SubmissionVisibility visibility;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (visibility == null) {
            visibility = SubmissionVisibility.PUBLIC;
        }
    }
}
