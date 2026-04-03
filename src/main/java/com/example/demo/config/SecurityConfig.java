package com.example.demo.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SupabaseJwtAuthenticationConverter supabaseJwtAuthenticationConverter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/lessons/mine").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/lessons/**").hasRole("CONTRIBUTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/lessons/**").hasRole("CONTRIBUTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/lessons/**").authenticated()
                        .requestMatchers("/api/**")
                        .access(authenticatedNonAdmin())
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(supabaseJwtAuthenticationConverter))
                );

        return http.build();
    }

    private WebExpressionAuthorizationManager authenticatedNonAdmin() {
        return new WebExpressionAuthorizationManager("isAuthenticated() and !hasRole('ADMIN')");
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${SUPABASE_JWT_SECRET:}") String jwtSecret
    ) {
        JwtDecoder jwksDecoder = null;
        JwtDecoder hmacDecoder = null;

        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            jwksDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                    .jwsAlgorithms(algorithms -> {
                        algorithms.add(SignatureAlgorithm.ES256);
                        algorithms.add(SignatureAlgorithm.RS256);
                    })
                    .build();
        }

        if (jwtSecret != null && !jwtSecret.isBlank()) {
            hmacDecoder = NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        if (jwksDecoder == null && hmacDecoder == null) {
            throw new IllegalStateException("Either SUPABASE_JWT_JWKS_URL or SUPABASE_JWT_SECRET is required.");
        }

        JwtDecoder finalJwksDecoder = jwksDecoder;
        JwtDecoder finalHmacDecoder = hmacDecoder;
        return token -> decodeWithFallback(token, finalJwksDecoder, finalHmacDecoder);
    }

    private Jwt decodeWithFallback(String token, JwtDecoder jwksDecoder, JwtDecoder hmacDecoder) {
        JwtException lastFailure = null;

        if (jwksDecoder != null) {
            try {
                return jwksDecoder.decode(token);
            } catch (JwtException ex) {
                lastFailure = ex;
            }
        }

        if (hmacDecoder != null) {
            try {
                return hmacDecoder.decode(token);
            } catch (JwtException ex) {
                if (lastFailure != null) {
                    ex.addSuppressed(lastFailure);
                }
                throw ex;
            }
        }

        throw lastFailure == null ? new JwtException("JWT decoder is not configured.") : lastFailure;
    }

}
