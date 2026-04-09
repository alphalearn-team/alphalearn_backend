package com.example.demo.game.lobby.invite;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.friendship.friend.FriendRepository;
import com.example.demo.game.lobby.GameLobby;
import com.example.demo.game.lobby.GameLobbyMemberRepository;
import com.example.demo.game.lobby.GameLobbyPhase;
import com.example.demo.game.lobby.GameLobbyRepository;
import com.example.demo.game.lobby.LearnerGameLobbyService;
import com.example.demo.game.lobby.dto.JoinPrivateGameLobbyRequest;
import com.example.demo.game.lobby.dto.JoinedPrivateGameLobbyDto;
import com.example.demo.game.lobby.invite.dto.GameLobbyInviteDto;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.notification.NotificationService;
import com.example.demo.notification.NotificationType;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameLobbyInviteService {

    private final GameLobbyInviteRepository inviteRepository;
    private final GameLobbyRepository lobbyRepository;
    private final GameLobbyMemberRepository lobbyMemberRepository;
    private final LearnerRepository learnerRepository;
    private final FriendRepository friendRepository;
    private final LearnerGameLobbyService learnerGameLobbyService;
    private final NotificationService notificationService;
    private final Clock clock;

    public GameLobbyInviteService(
            GameLobbyInviteRepository inviteRepository,
            GameLobbyRepository lobbyRepository,
            GameLobbyMemberRepository lobbyMemberRepository,
            LearnerRepository learnerRepository,
            FriendRepository friendRepository,
            LearnerGameLobbyService learnerGameLobbyService,
            NotificationService notificationService,
            Clock clock
    ) {
        this.inviteRepository = inviteRepository;
        this.lobbyRepository = lobbyRepository;
        this.lobbyMemberRepository = lobbyMemberRepository;
        this.learnerRepository = learnerRepository;
        this.friendRepository = friendRepository;
        this.learnerGameLobbyService = learnerGameLobbyService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public List<GameLobbyInviteDto> inviteFriends(
            SupabaseAuthUser user,
            UUID lobbyPublicId,
            List<UUID> friendPublicIds
    ) {
        SupabaseAuthUser learnerUser = requireLearner(user);

        if (friendPublicIds == null || friendPublicIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "friendPublicIds is required");
        }

        Set<UUID> uniqueFriendPublicIds = new LinkedHashSet<>(friendPublicIds.stream()
                .filter(id -> id != null)
                .toList());

        if (uniqueFriendPublicIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "friendPublicIds is required");
        }

        GameLobby lobby = resolveLobbyByPublicIdForUpdate(lobbyPublicId);
        ensureInvitableLobby(learnerUser, lobby);

        Learner sender = resolveLearnerById(learnerUser.userId());
        Map<UUID, Learner> receiverByPublicId = learnerRepository.findAllByPublicIdIn(uniqueFriendPublicIds)
                .stream()
                .collect(Collectors.toMap(Learner::getPublicId, Function.identity()));

        List<GameLobbyInviteDto> result = new ArrayList<>();
        for (UUID friendPublicId : uniqueFriendPublicIds) {
            Learner receiver = receiverByPublicId.get(friendPublicId);
            if (receiver == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found: " + friendPublicId);
            }
            if (receiver.getId().equals(learnerUser.userId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite yourself");
            }
            if (!friendRepository.existsFriendship(learnerUser.userId(), receiver.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only invite existing friends");
            }
            if (lobbyMemberRepository.existsByLobby_IdAndLearnerIdAndLeftAtIsNull(lobby.getId(), receiver.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend is already in this lobby");
            }

            GameLobbyInvite existingPendingInvite = inviteRepository
                    .findByLobby_IdAndReceiverLearnerIdAndStatus(lobby.getId(), receiver.getId(), GameLobbyInviteStatus.PENDING)
                    .orElse(null);
            if (existingPendingInvite != null) {
                result.add(toDto(existingPendingInvite, sender, receiver));
                continue;
            }

            GameLobbyInvite invite = new GameLobbyInvite();
            invite.setLobby(lobby);
            invite.setSenderLearnerId(learnerUser.userId());
            invite.setReceiverLearnerId(receiver.getId());
            invite.setStatus(GameLobbyInviteStatus.PENDING);
            invite.setCreatedAt(OffsetDateTime.now(clock));
            invite.setRespondedAt(null);

            GameLobbyInvite savedInvite = inviteRepository.save(invite);
            sendInviteNotification(savedInvite, lobby, sender, receiver);
            result.add(toDto(savedInvite, sender, receiver));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<GameLobbyInviteDto> listInvites(
            SupabaseAuthUser user,
            GameLobbyInviteDirection direction,
            GameLobbyInviteStatus status
    ) {
        SupabaseAuthUser learnerUser = requireLearner(user);
        GameLobbyInviteStatus inviteStatus = status == null ? GameLobbyInviteStatus.PENDING : status;

        List<GameLobbyInvite> invites = direction == GameLobbyInviteDirection.INCOMING
                ? inviteRepository.findByReceiverLearnerIdAndStatusOrderByCreatedAtDesc(learnerUser.userId(), inviteStatus)
                : inviteRepository.findBySenderLearnerIdAndStatusOrderByCreatedAtDesc(learnerUser.userId(), inviteStatus);

        Map<UUID, Learner> learnerById = resolveLearnersByInternalId(invites.stream()
                .flatMap(invite -> java.util.stream.Stream.of(invite.getSenderLearnerId(), invite.getReceiverLearnerId()))
                .collect(Collectors.toSet()));

        return invites.stream()
                .map(invite -> toDto(
                        invite,
                        learnerById.get(invite.getSenderLearnerId()),
                        learnerById.get(invite.getReceiverLearnerId())
                ))
                .toList();
    }

    @Transactional
    public GameLobbyInviteDto respondToInvite(SupabaseAuthUser user, UUID invitePublicId, String action) {
        SupabaseAuthUser learnerUser = requireLearner(user);
        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        return switch (normalizedAction) {
            case "ACCEPT" -> acceptInvite(learnerUser, invitePublicId);
            case "REJECT" -> rejectInvite(learnerUser, invitePublicId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be ACCEPT or REJECT");
        };
    }

    @Transactional
    public void cancelInvite(SupabaseAuthUser user, UUID invitePublicId) {
        SupabaseAuthUser learnerUser = requireLearner(user);
        GameLobbyInvite invite = inviteRepository.findByPublicIdForUpdate(invitePublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getSenderLearnerId().equals(learnerUser.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only invite sender can cancel invite");
        }
        if (invite.getStatus() != GameLobbyInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending invite can be canceled");
        }

        invite.setStatus(GameLobbyInviteStatus.CANCELED);
        invite.setRespondedAt(OffsetDateTime.now(clock));
        inviteRepository.save(invite);
    }

    private GameLobbyInviteDto acceptInvite(SupabaseAuthUser learnerUser, UUID invitePublicId) {
        GameLobbyInvite invite = inviteRepository.findByPublicIdForUpdate(invitePublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));

        validateReceiverAndPendingInvite(learnerUser, invite);

        GameLobby lobby = lobbyRepository.findById(invite.getLobby().getId()).orElse(null);
        if (lobby == null || !isLobbyJoinable(lobby)) {
            expireInvite(invite);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is no longer joinable");
        }

        JoinedPrivateGameLobbyDto joinedLobby = learnerGameLobbyService.joinPrivateLobby(
                learnerUser,
                new JoinPrivateGameLobbyRequest(lobby.getLobbyCode())
        );

        invite.setStatus(GameLobbyInviteStatus.ACCEPTED);
        invite.setRespondedAt(OffsetDateTime.now(clock));
        GameLobbyInvite saved = inviteRepository.save(invite);

        Learner receiver = resolveLearnerById(invite.getReceiverLearnerId());
        Learner sender = resolveLearnerById(invite.getSenderLearnerId());
        notificationService.create(
                sender.getId(),
                normalizeDisplayName(receiver) + " accepted your game invite.",
                NotificationType.GAME_LOBBY_INVITE_ACCEPTED,
                Map.of(
                        "invitePublicId", saved.getPublicId(),
                        "lobbyPublicId", joinedLobby.publicId(),
                        "lobbyCode", joinedLobby.lobbyCode(),
                        "receiverPublicId", receiver.getPublicId(),
                        "receiverUsername", normalizeDisplayName(receiver)
                )
        );

        return toDto(saved, sender, receiver);
    }

    private GameLobbyInviteDto rejectInvite(SupabaseAuthUser learnerUser, UUID invitePublicId) {
        GameLobbyInvite invite = inviteRepository.findByPublicIdForUpdate(invitePublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));

        validateReceiverAndPendingInvite(learnerUser, invite);

        invite.setStatus(GameLobbyInviteStatus.REJECTED);
        invite.setRespondedAt(OffsetDateTime.now(clock));
        GameLobbyInvite saved = inviteRepository.save(invite);

        Learner receiver = resolveLearnerById(invite.getReceiverLearnerId());
        Learner sender = resolveLearnerById(invite.getSenderLearnerId());

        notificationService.create(
                sender.getId(),
                normalizeDisplayName(receiver) + " rejected your game invite.",
                NotificationType.GAME_LOBBY_INVITE_REJECTED,
                Map.of(
                        "invitePublicId", saved.getPublicId(),
                        "lobbyPublicId", invite.getLobby().getPublicId(),
                        "receiverPublicId", receiver.getPublicId(),
                        "receiverUsername", normalizeDisplayName(receiver)
                )
        );

        return toDto(saved, sender, receiver);
    }

    private void validateReceiverAndPendingInvite(SupabaseAuthUser learnerUser, GameLobbyInvite invite) {
        if (!invite.getReceiverLearnerId().equals(learnerUser.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only invite receiver can respond to invite");
        }
        if (invite.getStatus() != GameLobbyInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite already handled");
        }
    }

    private void sendInviteNotification(GameLobbyInvite invite, GameLobby lobby, Learner sender, Learner receiver) {
        notificationService.create(
                receiver.getId(),
                normalizeDisplayName(sender) + " invited you to join game lobby " + lobby.getLobbyCode(),
                NotificationType.GAME_LOBBY_INVITE,
                Map.of(
                        "invitePublicId", invite.getPublicId(),
                        "lobbyPublicId", lobby.getPublicId(),
                        "lobbyCode", lobby.getLobbyCode(),
                        "senderPublicId", sender.getPublicId(),
                        "senderUsername", normalizeDisplayName(sender),
                        "actions", List.of("ACCEPT", "REJECT")
                )
        );
    }

    private GameLobbyInviteDto toDto(GameLobbyInvite invite, Learner sender, Learner receiver) {
        Learner safeSender = sender == null ? resolveLearnerById(invite.getSenderLearnerId()) : sender;
        Learner safeReceiver = receiver == null ? resolveLearnerById(invite.getReceiverLearnerId()) : receiver;
        return new GameLobbyInviteDto(
                invite.getPublicId(),
                invite.getLobby().getPublicId(),
                invite.getLobby().getLobbyCode(),
                safeSender.getPublicId(),
                normalizeDisplayName(safeSender),
                safeReceiver.getPublicId(),
                normalizeDisplayName(safeReceiver),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getRespondedAt(),
                invite.getExpiresAt()
        );
    }

    private SupabaseAuthUser requireLearner(SupabaseAuthUser user) {
        if (user == null || user.userId() == null || user.learner() == null || !user.isLearner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return user;
    }

    private GameLobby resolveLobbyByPublicIdForUpdate(UUID lobbyPublicId) {
        if (lobbyPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lobbyPublicId is required");
        }
        return lobbyRepository.findByPublicIdForUpdate(lobbyPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game lobby not found"));
    }

    private void ensureInvitableLobby(SupabaseAuthUser learnerUser, GameLobby lobby) {
        if (!lobby.isPrivateLobby()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only private lobby supports invites");
        }
        if (!learnerUser.userId().equals(lobby.getHostLearnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lobby host can invite friends");
        }
        if (!isLobbyJoinable(lobby)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is no longer joinable");
        }
    }

    private boolean isLobbyJoinable(GameLobby lobby) {
        if (lobby == null) {
            return false;
        }
        return lobby.getCurrentPhase() != GameLobbyPhase.ABANDONED
                && lobby.getCurrentPhase() != GameLobbyPhase.MATCH_COMPLETE;
    }

    private void expireInvite(GameLobbyInvite invite) {
        invite.setStatus(GameLobbyInviteStatus.EXPIRED);
        invite.setRespondedAt(OffsetDateTime.now(clock));
        inviteRepository.save(invite);
    }

    private Learner resolveLearnerById(UUID learnerId) {
        return learnerRepository.findById(learnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found"));
    }

    private Map<UUID, Learner> resolveLearnersByInternalId(Collection<UUID> learnerIds) {
        if (learnerIds == null || learnerIds.isEmpty()) {
            return Map.of();
        }
        return learnerRepository.findAllById(learnerIds).stream()
                .collect(Collectors.toMap(Learner::getId, Function.identity()));
    }

    private String normalizeDisplayName(Learner learner) {
        if (learner == null || learner.getUsername() == null || learner.getUsername().isBlank()) {
            return "Someone";
        }
        return learner.getUsername().trim();
    }
}
