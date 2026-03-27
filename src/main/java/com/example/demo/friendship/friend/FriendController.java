package com.example.demo.friendship.friend;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
        Learner learner = requireLearner(authentication);
        return friendService.getFriends(learner);
    }

    @DeleteMapping("/{friendPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(
            @PathVariable UUID friendPublicId,
            Authentication authentication
    ) {
            Learner currentLearner = requireLearner(authentication);
            friendService.removeFriend(currentLearner, friendPublicId);
    }

    private Learner requireLearner(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof SupabaseAuthUser authUser) || authUser.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return authUser.learner();
    }

}
