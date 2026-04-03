package com.example.demo.config;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class SupabaseAuthenticationToken extends AbstractAuthenticationToken {

    private final SupabaseAuthUser principal;
    private final Jwt token;

    public SupabaseAuthenticationToken(
            SupabaseAuthUser principal,
            Jwt token,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public SupabaseAuthUser getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        if (principal != null && principal.userId() != null) {
            return principal.userId().toString();
        }
        return token == null || token.getSubject() == null ? "" : token.getSubject();
    }
}
