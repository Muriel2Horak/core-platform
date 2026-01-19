-- =====================================================
-- U6: Rollback for V6__add_admin_tenant_grafana_binding.sql
-- =====================================================

DELETE FROM grafana_tenant_bindings
WHERE tenant_id = 'admin'
  AND grafana_org_id = 2;
