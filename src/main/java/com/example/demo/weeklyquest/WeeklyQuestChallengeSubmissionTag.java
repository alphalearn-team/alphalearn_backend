package com.example.demo.weeklyquest;

import com.example.demo.learner.Learner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weekly_quest_challenge_submission_tags")
public class WeeklyQuestChallengeSubmissionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private WeeklyQuestChallengeSubmission submission;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tagged_learner_id", nullable = false)
    private Learner taggedLearner;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public WeeklyQuestChallengeSubmissionTag(
            WeeklyQuestChallengeSubmission submission,
            Learner taggedLearner,
            OffsetDateTime createdAt
    ) {
        this.submission = submission;
        this.taggedLearner = taggedLearner;
        this.createdAt = createdAt;
    }
}
