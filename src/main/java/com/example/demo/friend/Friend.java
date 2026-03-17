package com.example.demo.friend;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "friends")
@IdClass(FriendId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friend {

    @Id
    @Column(name = "user_id_1")
    private UUID userId1;

    @Id
    @Column(name = "user_id_2")
    private UUID userId2;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}