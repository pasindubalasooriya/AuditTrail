package com.audittrail.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // turns on @PreAuthorize on controller methods
public class SecurityConfig {

    private final SecurityExceptionHandler securityExceptionHandler;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    public SecurityConfig(SecurityExceptionHandler securityExceptionHandler) {
        this.securityExceptionHandler = securityExceptionHandler;
    }

    /**
     * WSO2 issues access tokens as RFC 9068 "JWT access tokens", tagged with the
     * JOSE header {@code typ: at+jwt}. Spring's auto-configured decoder only accepts
     * {@code typ: JWT} (or no typ header) and rejects at+jwt before checking the
     * signature, so we build the decoder explicitly and widen the allowed types.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwtProcessorCustomizer(processor -> processor.setJWSTypeVerifier(
                        new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("at+jwt"), JOSEObjectType.JWT, null)))
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Allow the React dev origin (browser preflight + calls)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Token-based API: no cookies, no CSRF, no server session
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            // Validate the incoming JWT (signature via WSO2 JWKS, issuer, expiry) and map roles
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(securityExceptionHandler)   // 401 on bad/missing token
                .accessDeniedHandler(securityExceptionHandler)        // 403 at filter level
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityExceptionHandler)
                .accessDeniedHandler(securityExceptionHandler));
        return http.build();
    }

    /**
     * WSO2 puts roles in a "roles" claim, but the shape varies: a comma-separated
     * string for Application-audience roles, or a JSON array for Organization-audience
     * roles. Spring's hasRole('X') looks for authority "ROLE_X", so we normalize
     * either shape into a list and prefix each role with ROLE_.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object claim = jwt.getClaim("roles");
            List<String> roles;
            if (claim instanceof Collection<?> collection) {
                roles = collection.stream().map(String::valueOf).collect(Collectors.toList());
            } else if (claim instanceof String str) {
                roles = Arrays.asList(str.split(","));
            } else {
                roles = List.of();
            }
            Collection<GrantedAuthority> authorities = roles.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
            return authorities;
        });
        return converter;
    }

    /** CORS for the Vite dev server (http://localhost:5173). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
