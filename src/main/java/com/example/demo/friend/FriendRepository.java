package com.example.demo.friend;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendRepository extends JpaRepository<Friend, FriendId> {

        @Query("""
    SELECT f FROM Friend f
    WHERE f.userId1 = :userId OR f.userId2 = :userId
    """)
    List<Friend> findFriends(UUID userId);

}