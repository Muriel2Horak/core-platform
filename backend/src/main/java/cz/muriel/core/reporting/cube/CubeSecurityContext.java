package cz.muriel.core.reporting.cube;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds security context for Cube.js queries from Spring Security Authentication.
 */
@Slf4j
@Component
public class CubeSecurityContext {

    /**
     * Extract tenant ID from JWT claims.
     * 
     * @param authentication Spring Security Authentication
     * @return Tenant ID or null if not found
     */
    public String extractTenantId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            
            // Try tenant_id claim first
            Object tenantId = jwt.getClaim("tenant_id");
            if (tenantId != null) {
                return tenantId.toString();
            }
            
            // Fallback to tid claim
            Object tid = jwt.getClaim("tid");
            if (tid != null) {
                return tid.toString();
            }
            
            log.warn("No tenant_id claim found in JWT");
        }
        
        return null;
    }

    /**
     * Extract user ID from JWT claims.
     * 
     * @param authentication Spring Security Authentication
     * @return User ID or null if not found
     */
    public String extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            
            // Try sub claim (subject)
            String sub = jwt.getSubject();
            if (sub != null) {
                return sub;
            }
            
            // Fallback to user_id claim
            Object userId = jwt.getClaim("user_id");
            if (userId != null) {
                return userId.toString();
            }
        }
        
        return null;
    }

    /**
     * Extract roles from authentication.
     * 
     * @param authentication Spring Security Authentication
     * @return List of role names
     */
    public List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
            .collect(Collectors.toList());
    }

    /**
     * Build complete security context for Cube.js query.
     * 
     * @param authentication Spring Security Authentication
     * @return Security context map
     */
    public Map<String, Object> buildSecurityContext(Authentication authentication) {
        Map<String, Object> context = new HashMap<>();
        
        String tenantId = extractTenantId(authentication);
        if (tenantId != null) {
            context.put("tenantId", tenantId);
        }
        
        String userId = extractUserId(authentication);
        if (userId != null) {
            context.put("userId", userId);
        }
        
        List<String> roles = extractRoles(authentication);
        if (!roles.isEmpty()) {
            context.put("roles", roles);
        }
        
        return context;
    }
}
