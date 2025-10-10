package cz.muriel.core.reporting.security;

import cz.muriel.core.reporting.cube.CubeSecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Security service for validating access to reporting features.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingSecurityService {

    private final CubeSecurityContext cubeSecurityContext;

    // Entities that require admin role
    private static final Set<String> ADMIN_ONLY_ENTITIES = Set.of(
        "AuditLog",
        "SystemConfig",
        "TenantSettings"
    );

    // Sensitive fields that require elevated permissions
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password",
        "api_key",
        "secret",
        "token",
        "credit_card",
        "ssn"
    );

    /**
     * Check if user can query the specified entity.
     */
    public void validateEntityAccess(String entity, Authentication authentication) {
        List<String> roles = cubeSecurityContext.extractRoles(authentication);

        // Check admin-only entities
        if (ADMIN_ONLY_ENTITIES.contains(entity)) {
            if (!hasAdminRole(roles)) {
                log.warn("Access denied to admin-only entity: {}", entity);
                throw new AccessDeniedException("Access denied to entity: " + entity);
            }
        }

        log.debug("Entity access validated: entity={}, roles={}", entity, roles);
    }

    /**
     * Check if user can query sensitive fields.
     */
    public void validateFieldAccess(List<String> dimensions, List<String> measures, Authentication authentication) {
        List<String> roles = cubeSecurityContext.extractRoles(authentication);

        // Check dimensions
        if (dimensions != null) {
            for (String dimension : dimensions) {
                String fieldName = extractFieldName(dimension);
                if (SENSITIVE_FIELDS.contains(fieldName.toLowerCase())) {
                    if (!hasAdminRole(roles)) {
                        log.warn("Access denied to sensitive dimension: {}", dimension);
                        throw new AccessDeniedException("Access denied to sensitive field: " + fieldName);
                    }
                }
            }
        }

        // Check measures
        if (measures != null) {
            for (String measure : measures) {
                String fieldName = extractFieldName(measure);
                if (SENSITIVE_FIELDS.contains(fieldName.toLowerCase())) {
                    if (!hasAdminRole(roles)) {
                        log.warn("Access denied to sensitive measure: {}", measure);
                        throw new AccessDeniedException("Access denied to sensitive field: " + fieldName);
                    }
                }
            }
        }
    }

    /**
     * Validate row-level security filters are applied.
     */
    public void validateRowLevelSecurity(String tenantId, Authentication authentication) {
        String authTenantId = cubeSecurityContext.extractTenantId(authentication);

        if (!tenantId.equals(authTenantId)) {
            log.error("Tenant ID mismatch: request={}, auth={}", tenantId, authTenantId);
            throw new AccessDeniedException("Tenant ID mismatch");
        }
    }

    /**
     * Check if user has admin role.
     */
    private boolean hasAdminRole(List<String> roles) {
        return roles.stream()
            .anyMatch(role -> role.equals("TENANT_ADMIN") || role.equals("SUPER_ADMIN"));
    }

    /**
     * Extract field name from dimension/measure (e.g., "User.email" -> "email").
     */
    private String extractFieldName(String field) {
        if (field.contains(".")) {
            return field.substring(field.lastIndexOf('.') + 1);
        }
        return field;
    }

    /**
     * Sanitize SQL-like patterns to prevent injection.
     */
    public String sanitizePattern(String pattern) {
        if (pattern == null) {
            return null;
        }

        // Remove dangerous characters
        return pattern
            .replaceAll("[;'\"\\\\]", "")
            .trim();
    }

    /**
     * Validate query complexity to prevent DoS.
     */
    public void validateQueryComplexity(int dimensionCount, int measureCount, int filterCount) {
        final int MAX_DIMENSIONS = 20;
        final int MAX_MEASURES = 10;
        final int MAX_FILTERS = 50;

        if (dimensionCount > MAX_DIMENSIONS) {
            throw new IllegalArgumentException("Too many dimensions: " + dimensionCount + " (max: " + MAX_DIMENSIONS + ")");
        }

        if (measureCount > MAX_MEASURES) {
            throw new IllegalArgumentException("Too many measures: " + measureCount + " (max: " + MAX_MEASURES + ")");
        }

        if (filterCount > MAX_FILTERS) {
            throw new IllegalArgumentException("Too many filters: " + filterCount + " (max: " + MAX_FILTERS + ")");
        }
    }
}
