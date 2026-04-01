package com.example.demo.lesson;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.demo.concept.Concept;
import com.example.demo.contributor.Contributor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({"profile"}) //temporary fix to prevent loop, to review in the future
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    @Setter(lombok.AccessLevel.NONE)
    private Integer lessonId;

    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private JsonNode content;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, columnDefinition = "lessons_moderation_status")
    private LessonModerationStatus lessonModerationStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @ManyToMany
    @JoinTable(
            name = "lesson_concepts",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "concept_id")
    )
    private Set<Concept> concepts = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<LessonSection> sections = new java.util.ArrayList<>();

    public Lesson(
            String title,
            JsonNode content,
            LessonModerationStatus lessonModerationStatus,
            Contributor contributor,
            OffsetDateTime createdAt
    ) {
        this.title = title;
        this.content = content;
        this.lessonModerationStatus = lessonModerationStatus;
        this.contributor = contributor;
        this.createdAt = createdAt;
    }

    @PrePersist
    void assignPublicIdIfMissing() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
