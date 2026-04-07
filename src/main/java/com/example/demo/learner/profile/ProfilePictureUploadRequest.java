package com.example.demo.learner.profile;

public record ProfilePictureUploadRequest(
        String filename,
        String contentType,
        Long fileSizeBytes
) {}
