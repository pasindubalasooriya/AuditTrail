package com.audittrail.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helpers for reading the verified identity out of the JWT.
 * The token is validated by Spring Security before any of this runs, so these
 * values are trustworthy (cryptographically signed by WSO2), never user-supplied.
 */
@Component
public class JwtUtils {

    /** The verified username — the JWT 'sub' claim (configured in WSO2 to be the username). */
    public String getUsername(Authentication auth) {
        return auth.getName();
    }

    /** The raw validated JWT, for reading any claim. */
    public Jwt extractJwt(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    /**
     * WSO2 emits 'roles' as a single (possibly comma-separated) string, e.g. "FRAUD_ANALYST".
     * We split on commas so multiple roles are handled too.
     */
    public List<String> getRoles(Authentication auth) {
        Jwt jwt = extractJwt(auth);
        if (jwt == null) {
            return Collections.emptyList();
        }
        String roles = jwt.getClaimAsString("roles");
        if (roles == null || roles.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean hasRole(Authentication auth, String role) {
        return getRoles(auth).contains(role);
    }
}
