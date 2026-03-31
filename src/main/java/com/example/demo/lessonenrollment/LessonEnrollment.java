package com.example.demo.lessonenrollment;

import java.time.OffsetDateTime;

import com.example.demo.learner.Learner;
import com.example.demo.lesson.Lesson;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@JsonIgnoreProperties({"profile"}) //temporary fix to prevent loop, to review in the future
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lesson_enrollments")
@Schema(name = "LessonEnrollment", description = "Lesson enrollment row mapped from lesson_enrollments table")
public class LessonEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    @Setter(lombok.AccessLevel.NONE)
    @Schema(description = "Internal enrollment ID")
    private Integer enrollmentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    @Schema(description = "Learner linked to this enrollment")
    private Learner learner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    @Schema(description = "Lesson linked to this enrollment")
    private Lesson lesson;

    @Column(nullable = false)
    @Schema(description = "Whether the learner has completed this lesson")
    private boolean completed;

    @Column(name = "first_completed_at")
    @Schema(description = "Timestamp of first completion")
    private OffsetDateTime firstCompletedAt;
}
