package com.example.demo.quiz;

import java.time.OffsetDateTime;

import com.example.demo.learner.Learner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    @Setter(lombok.AccessLevel.NONE)
    private Integer attemptId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @Column(nullable = false)
    private short score;

    @Column(name = "is_first_attempt", nullable = false)
    private boolean isFirstAttempt;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt;

    public QuizAttempt(Quiz quiz, Learner learner, short score, boolean isFirstAttempt, OffsetDateTime attemptedAt) {
        this.quiz = quiz;
        this.learner = learner;
        this.score = score;
        this.isFirstAttempt = isFirstAttempt;
        this.attemptedAt = attemptedAt;
    }
}
