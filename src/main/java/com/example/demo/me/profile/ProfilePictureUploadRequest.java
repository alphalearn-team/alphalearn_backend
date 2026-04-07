package com.example.demo.me.profile;

public record ProfilePictureUploadRequest(
        String filename,
        String contentType,
        Long fileSizeBytes
) {}
