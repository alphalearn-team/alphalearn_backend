package com.example.demo.learner.profile;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.storage.r2.ProfilePictureStorageService;

@Service
public class MyProfileService {

    private final LearnerRepository learnerRepository;
    private final ProfilePictureStorageService profilePictureStorageService;

    public MyProfileService(
            LearnerRepository learnerRepository,
            ProfilePictureStorageService profilePictureStorageService
    ) {
        this.learnerRepository = learnerRepository;
        this.profilePictureStorageService = profilePictureStorageService;
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getProfile(SupabaseAuthUser user) {
        Learner learner = requireCurrentLearner(user);
        return MyProfileResponse.from(learner, user.email());
    }

    @Transactional
    public MyProfileResponse updateProfile(UpdateMyProfileRequest request, SupabaseAuthUser user) {
        Learner learner = requireCurrentLearner(user);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        if (request.username() != null) {
            String normalizedUsername = normalizeUsername(request.username());
            if (learnerRepository.existsByUsernameAndIdNot(normalizedUsername, learner.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
            }
            learner.setUsername(normalizedUsername);
        }

        if (request.bio() != null) {
            learner.setBio(normalizeBio(request.bio()));
        }

        return MyProfileResponse.from(learnerRepository.save(learner), user.email());
    }

    @Transactional(readOnly = true)
    public ProfilePictureUploadResponse createProfilePictureUploadInstruction(
            ProfilePictureUploadRequest request,
            SupabaseAuthUser user
    ) {
        Learner learner = requireCurrentLearner(user);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.filename() == null || request.filename().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "filename is required");
        }
        if (request.contentType() == null || request.contentType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        if (request.fileSizeBytes() == null || request.fileSizeBytes() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileSizeBytes must be greater than 0");
        }
        if (request.fileSizeBytes() > profilePictureStorageService.maxUploadSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "fileSizeBytes exceeds the maximum upload size");
        }
        if (!isSupportedImageType(request.contentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only image uploads are supported");
        }

        return ProfilePictureUploadResponse.from(
                profilePictureStorageService.generatePresignedUpload(
                        learner.getId(),
                        request.filename(),
                        request.contentType()
                )
        );
    }

    @Transactional
    public MyProfileResponse finalizeProfilePicture(FinalizeProfilePictureRequest request, SupabaseAuthUser user) {
        Learner learner = requireCurrentLearner(user);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.objectKey() == null || request.objectKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey is required");
        }

        String expectedPrefix = profilePictureStorageService.expectedObjectKeyPrefix(learner.getId());
        if (!request.objectKey().startsWith(expectedPrefix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey does not belong to the current learner profile picture upload");
        }

        ProfilePictureStorageService.StoredObjectMetadata metadata;
        try {
            metadata = profilePictureStorageService.fetchObjectMetadata(request.objectKey());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        if (!isSupportedImageType(metadata.contentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only image uploads are supported");
        }
        if (metadata.fileSizeBytes() > profilePictureStorageService.maxUploadSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded object exceeds the maximum upload size");
        }

        learner.setProfilePictureObjectKey(request.objectKey());
        learner.setProfilePicture(metadata.publicUrl());

        return MyProfileResponse.from(learnerRepository.save(learner), user.email());
    }

    private Learner requireCurrentLearner(SupabaseAuthUser user) {
        if (user == null || !user.isLearner() || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required");
        }
        return learnerRepository.findById(user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner account required"));
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? null : username.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username must not be blank");
        }
        return normalized;
    }

    private String normalizeBio(String bio) {
        String normalized = bio.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isSupportedImageType(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }
}
