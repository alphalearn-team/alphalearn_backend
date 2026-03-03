package com.example.demo.lesson.moderation;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonModerationStatus;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lesson_moderation_records")
public class LessonModerationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_moderation_record_id")
    @Setter(AccessLevel.NONE)
    private Integer lessonModerationRecordId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private LessonModerationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_source", nullable = false)
    private LessonModerationDecisionSource decisionSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_status", nullable = false, columnDefinition = "lessons_moderation_status")
    private LessonModerationStatus resultingStatus;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons", columnDefinition = "jsonb", nullable = false)
    private JsonNode reasons;

    @Column(name = "failure_message")
    private String failureMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private JsonNode rawResponse;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "actor_user_id", columnDefinition = "uuid")
    private UUID actorUserId;

    @Column(name = "provider_name")
    private String providerName;
}
