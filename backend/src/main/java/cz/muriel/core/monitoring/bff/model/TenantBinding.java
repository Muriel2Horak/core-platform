package cz.muriel.core.monitoring.bff.model;

public record TenantBinding(
    String tenantId,
    Long orgId,
    String serviceAccountToken
) {
}
