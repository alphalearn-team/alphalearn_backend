package com.example.demo.admin.me;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.SupabaseAuthUser;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping("/role")
    public UserRoleDto getRole(
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
