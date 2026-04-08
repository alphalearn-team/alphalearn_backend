package com.example.demo.learner.profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/me/profile")
@Tag(name = "My Profile", description = "Authenticated learner profile management. Password changes are handled in the frontend via Supabase Auth.")
public class MyProfileController {

    private final MyProfileService myProfileService;

    public MyProfileController(MyProfileService myProfileService) {
        this.myProfileService = myProfileService;
    }

    @GetMapping
    @Operation(summary = "Get my profile", description = "Returns the authenticated learner's profile details")
    public MyProfileResponse getMyProfile(@AuthenticationPrincipal SupabaseAuthUser user) {
        return myProfileService.getProfile(user);
    }

    @PatchMapping
    @Operation(summary = "Update my profile", description = "Updates the authenticated learner's username and bio")
    public MyProfileResponse updateMyProfile(
            @RequestBody UpdateMyProfileRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return myProfileService.updateProfile(request, user);
    }

    @PostMapping("/picture/uploads")
    @Operation(summary = "Create profile picture upload instructions", description = "Returns a presigned Cloudflare R2 upload URL for the authenticated learner's profile picture")
    public ProfilePictureUploadResponse createProfilePictureUpload(
            @RequestBody ProfilePictureUploadRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return myProfileService.createProfilePictureUploadInstruction(request, user);
    }

    @PutMapping("/picture")
    @Operation(summary = "Finalize profile picture", description = "Validates and stores the uploaded Cloudflare R2 profile picture for the authenticated learner")
    public MyProfileResponse finalizeProfilePicture(
            @RequestBody FinalizeProfilePictureRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return myProfileService.finalizeProfilePicture(request, user);
    }
}
