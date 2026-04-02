package com.example.demo.game.imposter.monthly;

import com.example.demo.concept.Concept;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "imposter_monthly_pack_weekly_features",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_imposter_pack_weekly_features_pack_week", columnNames = {"pack_id", "week_slot"}),
                @UniqueConstraint(name = "uk_imposter_pack_weekly_features_pack_concept", columnNames = {"pack_id", "concept_id"})
        }
)
public class ImposterMonthlyPackWeeklyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pack_id", nullable = false)
    private ImposterMonthlyPack pack;

    @ManyToOne(optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(name = "week_slot", nullable = false)
    private short weekSlot;
}
