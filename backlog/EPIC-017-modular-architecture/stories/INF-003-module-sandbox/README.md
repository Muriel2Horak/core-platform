---
id: INF-003
epic: EPIC-017-modular-architecture
title: "Module Sandbox Environment"
priority: P3
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-017-modular-architecture/stories/INF-003-module-sandbox/README.md
    - backlog/EPIC-017-modular-architecture/README.md
---

# INF-003: Module Sandbox Environment

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🟢 LOW  
**Dependencies:** MOD-002  
**Category:** Infrastructure

---

## 📖 User Story

**As a platform admin**,  
I want a sandbox environment to test new modules,  
So that I can verify modules before enabling for production tenants.

---

## 🎯 Acceptance Criteria

- ⏳ Sandbox tenant created automatically (`tenant:sandbox`)
- ⏳ Modules enabled in sandbox don't affect production tenants
- ⏳ Sandbox data isolated (separate DB schema or soft delete flag)
- ⏳ Admin can reset sandbox (delete all data, reinstall modules)
- ⏳ Promote module from sandbox to production (one-click)

---

## 💻 Implementation

### Sandbox Tenant Creation

```java
@Service
public class SandboxManager {
    
    private static final String SANDBOX_TENANT_ID = "sandbox";
    
    private final TenantRepository tenantRepository;
    private final ModuleRegistry moduleRegistry;
    
    @PostConstruct
    public void ensureSandboxExists() {
        Optional<Tenant> sandbox = tenantRepository.findById(SANDBOX_TENANT_ID);
        
        if (sandbox.isEmpty()) {
            Tenant sandboxTenant = new Tenant();
            sandboxTenant.setId(SANDBOX_TENANT_ID);
            sandboxTenant.setName("Sandbox");
            sandboxTenant.setIsSandbox(true);
            tenantRepository.save(sandboxTenant);
            
            log.info("✅ Sandbox tenant created");
        }
    }
    
    /**
     * Enable module in sandbox for testing.
     */
    public void enableInSandbox(String moduleId) {
        moduleRegistry.enableModule(moduleId, SANDBOX_TENANT_ID);
        log.info("Module {} enabled in sandbox", moduleId);
    }
    
    /**
     * Reset sandbox: delete all data, disable all modules.
     */
    public void resetSandbox() {
        // 1. Disable all modules
        moduleRegistry.getModulesByTenant(SANDBOX_TENANT_ID).forEach(module -> {
            moduleRegistry.disableModule(module.getId(), SANDBOX_TENANT_ID);
        });
        
        // 2. Delete all sandbox data (soft delete by tenant_id)
        jdbcTemplate.update("DELETE FROM entities WHERE tenant_id = ?", SANDBOX_TENANT_ID);
        jdbcTemplate.update("DELETE FROM workflow_instances WHERE tenant_id = ?", SANDBOX_TENANT_ID);
        
        log.info("✅ Sandbox reset completed");
    }
    
    /**
     * Promote module from sandbox to production tenant.
     */
    public void promoteToProduction(String moduleId, String targetTenantId) {
        // 1. Verify module works in sandbox
        ModuleInfo sandboxModule = moduleRegistry.getModule(moduleId, SANDBOX_TENANT_ID)
            .orElseThrow(() -> new IllegalStateException("Module not in sandbox"));
        
        if (sandboxModule.getStatus() != ModuleStatus.ENABLED) {
            throw new IllegalStateException("Module not enabled in sandbox");
        }
        
        // 2. Enable for production tenant
        moduleRegistry.enableModule(moduleId, targetTenantId);
        
        log.info("✅ Module {} promoted to tenant {}", moduleId, targetTenantId);
    }
}
```

---

## 🎨 UI: Sandbox Control Panel

```tsx
// SandboxControlPanel.tsx

import React from 'react';
import { Button, Card, CardContent } from '@mui/material';
import { sandboxApi } from '@/api/sandbox';

export const SandboxControlPanel: React.FC = () => {
  const handleReset = async () => {
    if (confirm('Reset sandbox? All data will be deleted.')) {
      await sandboxApi.reset();
      alert('Sandbox reset!');
    }
  };
  
  const handlePromote = async (moduleId: string) => {
    const tenantId = prompt('Enter production tenant ID:');
    if (tenantId) {
      await sandboxApi.promote(moduleId, tenantId);
      alert(`Module ${moduleId} promoted to ${tenantId}`);
    }
  };
  
  return (
    <Card>
      <CardContent>
        <h2>Sandbox Environment</h2>
        <p>Test new modules safely before production deployment.</p>
        
        <Button onClick={handleReset} color="warning">
          Reset Sandbox
        </Button>
        
        <Button onClick={() => handlePromote('helpdesk')} color="primary">
          Promote to Production
        </Button>
      </CardContent>
    </Card>
  );
};
```

---

## 📊 Success Metrics

- Sandbox uptime: 99%
- Module test coverage: >80% before production
- False positives (sandbox works, prod fails): <5%

---

**Last Updated:** 9. listopadu 2025
