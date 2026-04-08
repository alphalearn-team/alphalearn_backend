package com.example.demo.learner.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Me", description = "Authenticated utility endpoints for current user context")
public class MeController {

    @GetMapping
    @Operation(summary = "Get my profile context", description = "Returns authenticated user context including resolved role from JWT and role tables")
    public UserRoleDto getMe(
            Authentication authentication,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        boolean isAdmin = hasAuthority(authentication, "ROLE_ADMIN");
        boolean isContributor = hasAuthority(authentication, "ROLE_CONTRIBUTOR");
        boolean isLearner = user != null && user.isLearner() && !isContributor;

        String role = isAdmin
                ? "ADMIN"
                : isContributor
                ? "CONTRIBUTOR"
                : isLearner
                ? "LEARNER"
                : "AUTHENTICATED";

        UUID userId = user != null ? user.userId() : null;
        return new UserRoleDto(userId, role);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
