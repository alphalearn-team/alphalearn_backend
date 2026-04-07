package com.example.demo.me.profile;

import java.util.UUID;

import com.example.demo.learner.Learner;

public record MyProfileResponse(
        UUID publicId,
        String username,
        String bio,
        String profilePictureUrl,
        String email
) {
    public static MyProfileResponse from(Learner learner, String email) {
        return new MyProfileResponse(
                learner.getPublicId(),
                learner.getUsername(),
                learner.getBio(),
                learner.getProfilePicture(),
                email
        );
    }
}
