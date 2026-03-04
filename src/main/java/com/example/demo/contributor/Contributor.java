package com.example.demo.contributor;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.learner.Learner;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties("learner") //temporary fix to prevent loop, to review in the future
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contributors")
public class Contributor {

    @Id
    @Column(name = "contributor_id", columnDefinition = "uuid")
    private UUID contributorId;

   @MapsId
   @OneToOne(optional = false)
   @JoinColumn(name = "contributor_id", referencedColumnName = "id")
   private Learner learner;

    @Column(name = "promoted_at", nullable = false)
    private OffsetDateTime promotedAt;

    @Column(name = "demoted_at")
    private OffsetDateTime demotedAt;

    public Contributor(Learner learner, OffsetDateTime promotedAt) {
        this.learner = learner;
        this.promotedAt = promotedAt;
        this.demotedAt = null;
    }

    public boolean isCurrentContributor() {
        return promotedAt != null && (demotedAt == null || promotedAt.isAfter(demotedAt));
    }
}
