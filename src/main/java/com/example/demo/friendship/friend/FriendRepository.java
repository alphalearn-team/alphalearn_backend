package com.example.demo.friendship.friend;

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

    @Query("""
            select (count(f) > 0)
            from Friend f
            where (f.userId1 = :firstUserId and f.userId2 = :secondUserId)
               or (f.userId2 = :firstUserId and f.userId1 = :secondUserId)
    """)
    boolean existsFriendship(UUID firstUserId, UUID secondUserId);

}