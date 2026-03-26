package com.example.demo.friendship.friend;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.dto.FriendPublicDTO;
import com.example.demo.learner.Learner;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public List<FriendPublicDTO> getFriends(Authentication authentication) {

        SupabaseAuthUser authUser = (SupabaseAuthUser) authentication.getPrincipal();

        Learner learner = authUser.learner();

        return friendService.getFriends(learner);
    }

    @DeleteMapping("/{friendPublicId}")
        public void removeFriend(
                @PathVariable UUID friendPublicId,
                Authentication authentication
        ) {

            SupabaseAuthUser authUser = (SupabaseAuthUser) authentication.getPrincipal();
            Learner currentLearner = authUser.learner();

            friendService.removeFriend(currentLearner, friendPublicId);
    }

}