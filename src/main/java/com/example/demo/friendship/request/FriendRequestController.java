package com.example.demo.friendship.request;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.request.dto.FriendRequestDTO;
import com.example.demo.learner.Learner;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;
    
    @PostMapping("/{receiverPublicId}")
        public FriendRequestDTO sendRequest(
                @PathVariable UUID receiverPublicId,
                Authentication auth
        ) {
            Learner currentUser = ((SupabaseAuthUser) auth.getPrincipal()).learner();
            return friendRequestService.sendRequest(currentUser, receiverPublicId);
    }

        @GetMapping("/incoming")
        public List<FriendRequestDTO> getIncoming(Authentication auth) {
            Learner currentUser = ((SupabaseAuthUser) auth.getPrincipal()).learner();
            return friendRequestService.getPendingRequests(currentUser);
    }

        @GetMapping("/outgoing")
        public List<FriendRequestDTO> getOutgoing(Authentication auth) {

            Learner currentUser = ((SupabaseAuthUser) auth.getPrincipal()).learner();

            return friendRequestService.getOutgoingRequests(currentUser);
    }

    @PostMapping("/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptRequest(
            @PathVariable Long requestId,
            Authentication auth
    ) {

        Learner currentUser = ((SupabaseAuthUser) auth.getPrincipal()).learner();

        friendRequestService.acceptRequest(currentUser, requestId);
    }

    @PostMapping("/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(
            @PathVariable Long requestId,
            Authentication auth
    ) {

        Learner currentUser = ((SupabaseAuthUser) auth.getPrincipal()).learner();

        friendRequestService.rejectRequest(currentUser, requestId);
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRequest(
            @PathVariable Long requestId,
            Authentication auth
    ) {

        Learner currentUser = ((SupabaseAuthUser) auth.getPrincipal()).learner();

        friendRequestService.cancelRequest(currentUser, requestId);
    }
}
