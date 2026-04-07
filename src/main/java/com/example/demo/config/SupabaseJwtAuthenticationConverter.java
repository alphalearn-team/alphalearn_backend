package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.example.demo.admin.AdminRepository;
import com.example.demo.contributor.Contributor;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;

@Component
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ContributorRepository contributorRepository;
    private final LearnerRepository learnerRepository;
    private final AdminRepository adminRepository;

    public SupabaseJwtAuthenticationConverter(
            ContributorRepository contributorRepository,
            LearnerRepository learnerRepository,
            AdminRepository adminRepository
    ) {
        this.contributorRepository = contributorRepository;
        this.learnerRepository = learnerRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));

        UUID userId = parseSubject(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        Learner learner = null;
        Contributor contributor = null;
        if (userId != null) {
            boolean isAdmin = adminRepository.existsById(userId);
            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else {
                learner = learnerRepository.findById(userId).orElse(null);
                contributor = contributorRepository.findById(userId).orElse(null);
                if (contributor != null && contributor.isCurrentContributor()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR"));
                }
            }
        }

        SupabaseAuthUser authUser = new SupabaseAuthUser(userId, learner, contributor, email);
        return new SupabaseAuthenticationToken(authUser, jwt, authorities);
    }

    private UUID parseSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
