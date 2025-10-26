-- V7: Remove Grafana integration (standalone migration)
-- Author: GitHub Copilot
-- Date: 2025-10-26
-- Reason: Grafana moved to standalone ops tool with OIDC SSO, no longer embedded in FE

-- Drop Grafana tenant bindings table
DROP TABLE IF EXISTS grafana_tenant_bindings CASCADE;

-- Note: Grafana now operates as standalone monitoring tool at ops.core-platform.local
-- with direct OIDC integration to Keycloak. No tenant-org mapping needed.
