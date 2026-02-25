package com.example.demo.learner;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "learners")
public class Learner {

    @Id
    @Column(columnDefinition= "uuid")
    private UUID id;

    @Setter
    @Column(name = "public_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID publicId;

    @Setter
    @Column(unique= true)
    private String username;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "total_points", nullable = false)
    private short totalPoints;
}
