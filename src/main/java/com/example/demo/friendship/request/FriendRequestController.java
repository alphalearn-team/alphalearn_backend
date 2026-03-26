package com.example.demo.friendship.request;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.request.dto.CreateFriendRequestRequest;
import com.example.demo.friendship.request.dto.FriendRequestDTO;
import com.example.demo.friendship.request.dto.UpdateFriendRequestStatusRequest;
import com.example.demo.learner.Learner;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;
    
    @PostMapping
    public FriendRequestDTO sendRequest(
            @RequestBody CreateFriendRequestRequest request,
            Authentication auth
    ) {
            Learner currentUser = requireLearner(auth);
            return friendRequestService.sendRequest(currentUser, request == null ? null : request.receiverPublicId());
    }

    @GetMapping
    public List<FriendRequestDTO> getRequests(
            @RequestParam String direction,
            Authentication auth
    ) {
            Learner currentUser = requireLearner(auth);
            FriendRequestDirection parsedDirection;
            try {
                parsedDirection = FriendRequestDirection.fromQueryValue(direction);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            return switch (parsedDirection) {
                case INCOMING -> friendRequestService.getPendingRequests(currentUser);
                case OUTGOING -> friendRequestService.getOutgoingRequests(currentUser);
            };
    }

    @PatchMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRequestStatus(
            @PathVariable Long requestId,
            @RequestBody UpdateFriendRequestStatusRequest request,
            Authentication auth
    ) {
        Learner currentUser = requireLearner(auth);
        friendRequestService.updateRequestStatus(currentUser, requestId, request == null ? null : request.status());
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRequest(
            @PathVariable Long requestId,
            Authentication auth
    ) {
        Learner currentUser = requireLearner(auth);
        friendRequestService.cancelRequest(currentUser, requestId);
    }

    private Learner requireLearner(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof SupabaseAuthUser authUser) || authUser.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return authUser.learner();
    }
}
