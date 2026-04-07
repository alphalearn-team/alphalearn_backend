package com.example.demo.me.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.storage.r2.ProfilePictureStorageService;

@ExtendWith(MockitoExtension.class)
class MyProfileServiceTest {

    @Mock
    private LearnerRepository learnerRepository;
    @Mock
    private ProfilePictureStorageService profilePictureStorageService;

    private MyProfileService service;
    private Learner learner;
    private SupabaseAuthUser learnerUser;

    @BeforeEach
    void setUp() {
        service = new MyProfileService(learnerRepository, profilePictureStorageService);
        UUID learnerId = UUID.randomUUID();
        learner = new Learner(learnerId, UUID.randomUUID(), "learner", OffsetDateTime.parse("2026-03-01T00:00:00Z"), (short) 0);
        learner.setBio("Existing bio");
        learner.setProfilePicture("https://pub.example/profile-pictures/current.png");
        learner.setProfilePictureObjectKey("profile-pictures/current.png");
        learnerUser = new SupabaseAuthUser(learnerId, learner, null, "learner@example.com");

        when(learnerRepository.findById(learnerId)).thenReturn(Optional.of(learner));
    }

    @Test
    void returnsProfileWithEmailFromAuthUser() {
        MyProfileResponse response = service.getProfile(learnerUser);

        assertThat(response.username()).isEqualTo("learner");
        assertThat(response.bio()).isEqualTo("Existing bio");
        assertThat(response.profilePictureUrl()).isEqualTo("https://pub.example/profile-pictures/current.png");
        assertThat(response.email()).isEqualTo("learner@example.com");
    }

    @Test
    void updatesTrimmedUsernameAndNormalizedBio() {
        when(learnerRepository.existsByUsernameAndIdNot("new-name", learner.getId())).thenReturn(false);
        when(learnerRepository.save(any(Learner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MyProfileResponse response = service.updateProfile(
                new UpdateMyProfileRequest("  new-name  ", "   "),
                learnerUser
        );

        assertThat(response.username()).isEqualTo("new-name");
        assertThat(response.bio()).isNull();
        assertThat(learner.getUsername()).isEqualTo("new-name");
        assertThat(learner.getBio()).isNull();
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> service.updateProfile(new UpdateMyProfileRequest("   ", null), learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("username must not be blank");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(learnerRepository.existsByUsernameAndIdNot("taken-name", learner.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.updateProfile(new UpdateMyProfileRequest("taken-name", null), learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void createsProfilePictureUploadInstructions() {
        when(profilePictureStorageService.maxUploadSizeBytes()).thenReturn(1024L * 1024L);
        when(profilePictureStorageService.generatePresignedUpload(learner.getId(), "avatar.png", "image/png"))
                .thenReturn(new ProfilePictureStorageService.PresignedUpload(
                        "profile-pictures/%s/avatar.png".formatted(learner.getId()),
                        "https://pub.example/profile-pictures/avatar.png",
                        "https://signed.example",
                        OffsetDateTime.parse("2026-03-14T10:15:00Z"),
                        Map.of("Content-Type", "image/png")
                ));

        ProfilePictureUploadResponse response = service.createProfilePictureUploadInstruction(
                new ProfilePictureUploadRequest("avatar.png", "image/png", 4096L),
                learnerUser
        );

        assertThat(response.uploadUrl()).isEqualTo("https://signed.example");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", "image/png");
    }

    @Test
    void rejectsUnsupportedUploadType() {
        when(profilePictureStorageService.maxUploadSizeBytes()).thenReturn(1024L * 1024L);

        assertThatThrownBy(() -> service.createProfilePictureUploadInstruction(
                new ProfilePictureUploadRequest("avatar.pdf", "application/pdf", 4096L),
                learnerUser
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void rejectsOversizedUploadType() {
        when(profilePictureStorageService.maxUploadSizeBytes()).thenReturn(1024L);

        assertThatThrownBy(() -> service.createProfilePictureUploadInstruction(
                new ProfilePictureUploadRequest("avatar.png", "image/png", 2048L),
                learnerUser
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("413 PAYLOAD_TOO_LARGE");
    }

    @Test
    void finalizesProfilePictureAfterValidatingObject() {
        String objectKey = "profile-pictures/%s/avatar.png".formatted(learner.getId());
        when(profilePictureStorageService.expectedObjectKeyPrefix(learner.getId()))
                .thenReturn("profile-pictures/%s/".formatted(learner.getId()));
        when(profilePictureStorageService.fetchObjectMetadata(objectKey))
                .thenReturn(new ProfilePictureStorageService.StoredObjectMetadata(
                        "image/png",
                        4096L,
                        "https://pub.example/" + objectKey
                ));
        when(profilePictureStorageService.maxUploadSizeBytes()).thenReturn(1024L * 1024L);
        when(learnerRepository.save(any(Learner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MyProfileResponse response = service.finalizeProfilePicture(new FinalizeProfilePictureRequest(objectKey), learnerUser);

        assertThat(response.profilePictureUrl()).isEqualTo("https://pub.example/" + objectKey);
        assertThat(learner.getProfilePictureObjectKey()).isEqualTo(objectKey);
        assertThat(learner.getProfilePicture()).isEqualTo("https://pub.example/" + objectKey);
    }

    @Test
    void rejectsProfilePictureFinalizeOutsideLearnerNamespace() {
        when(profilePictureStorageService.expectedObjectKeyPrefix(learner.getId()))
                .thenReturn("profile-pictures/%s/".formatted(learner.getId()));

        assertThatThrownBy(() -> service.finalizeProfilePicture(
                new FinalizeProfilePictureRequest("profile-pictures/other/avatar.png"),
                learnerUser
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("objectKey does not belong");
    }

    @Test
    void rejectsProfilePictureFinalizeWhenObjectMissing() {
        String objectKey = "profile-pictures/%s/avatar.png".formatted(learner.getId());
        when(profilePictureStorageService.expectedObjectKeyPrefix(learner.getId()))
                .thenReturn("profile-pictures/%s/".formatted(learner.getId()));
        when(profilePictureStorageService.fetchObjectMetadata(objectKey))
                .thenThrow(new IllegalArgumentException("Uploaded object was not found"));

        assertThatThrownBy(() -> service.finalizeProfilePicture(new FinalizeProfilePictureRequest(objectKey), learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Uploaded object was not found");
    }

    @Test
    void rejectsProfilePictureFinalizeWhenObjectIsNotImage() {
        String objectKey = "profile-pictures/%s/avatar.png".formatted(learner.getId());
        when(profilePictureStorageService.expectedObjectKeyPrefix(learner.getId()))
                .thenReturn("profile-pictures/%s/".formatted(learner.getId()));
        when(profilePictureStorageService.fetchObjectMetadata(objectKey))
                .thenReturn(new ProfilePictureStorageService.StoredObjectMetadata(
                        "application/pdf",
                        4096L,
                        "https://pub.example/" + objectKey
                ));

        assertThatThrownBy(() -> service.finalizeProfilePicture(new FinalizeProfilePictureRequest(objectKey), learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void rejectsProfilePictureFinalizeWhenObjectIsTooLarge() {
        String objectKey = "profile-pictures/%s/avatar.png".formatted(learner.getId());
        when(profilePictureStorageService.expectedObjectKeyPrefix(learner.getId()))
                .thenReturn("profile-pictures/%s/".formatted(learner.getId()));
        when(profilePictureStorageService.fetchObjectMetadata(objectKey))
                .thenReturn(new ProfilePictureStorageService.StoredObjectMetadata(
                        "image/png",
                        4096L,
                        "https://pub.example/" + objectKey
                ));
        when(profilePictureStorageService.maxUploadSizeBytes()).thenReturn(1024L);

        assertThatThrownBy(() -> service.finalizeProfilePicture(new FinalizeProfilePictureRequest(objectKey), learnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("413 PAYLOAD_TOO_LARGE");
    }

    @Test
    void rejectsProfileAccessWhenLearnerMissing() {
        SupabaseAuthUser noLearnerUser = new SupabaseAuthUser(UUID.randomUUID(), null, null, "user@example.com");

        assertThatThrownBy(() -> service.getProfile(noLearnerUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }
}
