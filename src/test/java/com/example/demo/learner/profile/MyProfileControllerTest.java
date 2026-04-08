package com.example.demo.learner.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.config.SupabaseAuthenticationToken;
import com.example.demo.learner.Learner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class MyProfileControllerTest {

    @Mock
    private MyProfileService myProfileService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SupabaseAuthUser learnerUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new MyProfileController(myProfileService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UUID learnerId = UUID.randomUUID();
        Learner learner = new Learner(learnerId, UUID.randomUUID(), "learner", OffsetDateTime.parse("2026-03-01T00:00:00Z"), (short) 0);
        learnerUser = new SupabaseAuthUser(learnerId, learner, null, "learner@example.com");
        setAuthentication(learnerUser);
    }

    @Test
    void returnsMyProfile() throws Exception {
        when(myProfileService.getProfile(any())).thenReturn(new MyProfileResponse(
                UUID.randomUUID(),
                "learner",
                "Bio text",
                "https://pub.example/profile-pictures/learner/avatar.png",
                "learner@example.com"
        ));

        mockMvc.perform(get("/api/me/profile").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("learner"))
                .andExpect(jsonPath("$.bio").value("Bio text"))
                .andExpect(jsonPath("$.profilePictureUrl").value("https://pub.example/profile-pictures/learner/avatar.png"))
                .andExpect(jsonPath("$.email").value("learner@example.com"));
    }

    @Test
    void rejectsGetWhenLearnerMissing() throws Exception {
        when(myProfileService.getProfile(any())).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Learner account required"
        ));

        SupabaseAuthUser userWithoutLearner = new SupabaseAuthUser(UUID.randomUUID(), null, null, "user@example.com");
        setAuthentication(userWithoutLearner);

        mockMvc.perform(get("/api/me/profile").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatesMyProfile() throws Exception {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(" next-user ", "Updated bio");
        when(myProfileService.updateProfile(eq(request), any())).thenReturn(new MyProfileResponse(
                UUID.randomUUID(),
                "next-user",
                "Updated bio",
                null,
                "learner@example.com"
        ));

        mockMvc.perform(patch("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("next-user"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));
    }

    @Test
    void rejectsBlankUsernameUpdate() throws Exception {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("   ", null);
        when(myProfileService.updateProfile(eq(request), any())).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "username must not be blank"
        ));

        mockMvc.perform(patch("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateUsernameUpdate() throws Exception {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("taken-name", null);
        when(myProfileService.updateProfile(eq(request), any())).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Username is already taken"
        ));

        mockMvc.perform(patch("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsProfilePictureUploadInstructions() throws Exception {
        ProfilePictureUploadRequest request = new ProfilePictureUploadRequest("avatar.png", "image/png", 4096L);
        when(myProfileService.createProfilePictureUploadInstruction(eq(request), any())).thenReturn(new ProfilePictureUploadResponse(
                "profile-pictures/learner/avatar.png",
                "https://pub.example/profile-pictures/learner/avatar.png",
                "https://signed.example",
                OffsetDateTime.parse("2026-03-14T10:15:00Z"),
                Map.of("Content-Type", "image/png")
        ));

        mockMvc.perform(post("/api/me/profile/picture/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("profile-pictures/learner/avatar.png"))
                .andExpect(jsonPath("$.uploadUrl").value("https://signed.example"))
                .andExpect(jsonPath("$.requiredHeaders.Content-Type").value("image/png"));
    }

    @Test
    void rejectsUnsupportedProfilePictureUpload() throws Exception {
        ProfilePictureUploadRequest request = new ProfilePictureUploadRequest("avatar.pdf", "application/pdf", 4096L);
        when(myProfileService.createProfilePictureUploadInstruction(eq(request), any())).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only image uploads are supported"
        ));

        mockMvc.perform(post("/api/me/profile/picture/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void finalizesProfilePicture() throws Exception {
        FinalizeProfilePictureRequest request = new FinalizeProfilePictureRequest("profile-pictures/learner/avatar.png");
        when(myProfileService.finalizeProfilePicture(eq(request), any())).thenReturn(new MyProfileResponse(
                UUID.randomUUID(),
                "learner",
                "Bio text",
                "https://pub.example/profile-pictures/learner/avatar.png",
                "learner@example.com"
        ));

        mockMvc.perform(put("/api/me/profile/picture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePictureUrl").value("https://pub.example/profile-pictures/learner/avatar.png"));
    }

    private void setAuthentication(SupabaseAuthUser user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SupabaseAuthenticationToken(
                user,
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject(user.userId() == null ? UUID.randomUUID().toString() : user.userId().toString())
                        .claim("email", user.email())
                        .build(),
                List.of()
        ));
        SecurityContextHolder.setContext(context);
    }
}
