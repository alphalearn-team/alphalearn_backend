package com.example.demo.weeklyquest;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.weeklyquest.enums.QuestSubmissionMode;
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
@Table(name = "quest_templates")
public class QuestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    @Setter(lombok.AccessLevel.NONE)
    private UUID publicId;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(name = "instruction_text", columnDefinition = "text", nullable = false)
    private String instructionText;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_mode", nullable = false, columnDefinition = "quest_submission_mode")
    private QuestSubmissionMode submissionMode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
