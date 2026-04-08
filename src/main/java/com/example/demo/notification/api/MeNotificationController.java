package com.example.demo.notification.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.notification.NotificationDto;
import com.example.demo.notification.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/me/notifications")
@Tag(name = "Me – Notifications", description = "Fetch and manage the current user's notifications")
public class MeNotificationController {

    private final NotificationService notificationService;

    public MeNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get my notifications", description = "Returns all notifications for the authenticated learner, newest first")
    public List<NotificationDto> getMyNotifications(@AuthenticationPrincipal SupabaseAuthUser user) {
        UUID learnerInternalId = requireLearnerInternalId(user);
        return notificationService.getForLearner(learnerInternalId);
    }

    @PatchMapping("/{publicId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a notification as read")
    public void markRead(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        UUID learnerInternalId = requireLearnerInternalId(user);
        notificationService.markRead(learnerInternalId, publicId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all notifications as read")
    public void markAllRead(@AuthenticationPrincipal SupabaseAuthUser user) {
        UUID learnerInternalId = requireLearnerInternalId(user);
        notificationService.markAllRead(learnerInternalId);
    }

    private UUID requireLearnerInternalId(SupabaseAuthUser user) {
        if (user == null || user.learner() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user.learner().getId();
    }
}
