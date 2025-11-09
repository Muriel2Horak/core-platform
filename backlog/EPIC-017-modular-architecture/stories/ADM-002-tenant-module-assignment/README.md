# ADM-002: Tenant Module Assignment

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** ADM-001, LIC-001  
**Category:** Admin UI

---

## 📖 User Story

**As a platform admin**,  
I want to assign modules to specific tenants,  
So that each tenant has only modules they licensed.

---

## 🎯 Acceptance Criteria

- ⏳ Tenant × Module matrix UI (DataGrid)
- ⏳ Enable/disable module per tenant
- ⏳ Upload license for tenant + module
- ⏳ Show license status (valid, expiring, expired)
- ⏳ Bulk operations (enable for all tenants)

---

## 💻 Implementation

### React Component

```tsx
// TenantModuleMatrix.tsx

import React, { useState, useEffect } from 'react';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { Switch, Chip, Button } from '@mui/material';
import { tenantApi } from '@/api/tenants';
import { moduleApi } from '@/api/modules';

export const TenantModuleMatrix: React.FC = () => {
  const [tenants, setTenants] = useState([]);
  const [modules, setModules] = useState([]);
  const [assignments, setAssignments] = useState<Record<string, boolean>>({});
  
  useEffect(() => {
    loadData();
  }, []);
  
  const loadData = async () => {
    const [tenantsData, modulesData, assignmentsData] = await Promise.all([
      tenantApi.list(),
      moduleApi.list({ status: 'ENABLED' }),
      tenantApi.getModuleAssignments()
    ]);
    
    setTenants(tenantsData);
    setModules(modulesData);
    setAssignments(assignmentsData);
  };
  
  const handleToggle = async (tenantId: string, moduleId: string) => {
    const key = `${tenantId}:${moduleId}`;
    const newValue = !assignments[key];
    
    if (newValue) {
      await tenantApi.enableModule(tenantId, moduleId);
    } else {
      await tenantApi.disableModule(tenantId, moduleId);
    }
    
    setAssignments({ ...assignments, [key]: newValue });
  };
  
  const columns: GridColDef[] = [
    { field: 'tenant', headerName: 'Tenant', width: 200 },
    ...modules.map(module => ({
      field: module.id,
      headerName: module.name,
      width: 150,
      renderCell: (params) => {
        const key = `${params.row.id}:${module.id}`;
        return (
          <Switch
            checked={assignments[key] || false}
            onChange={() => handleToggle(params.row.id, module.id)}
          />
        );
      }
    }))
  ];
  
  const rows = tenants.map(tenant => ({
    id: tenant.id,
    tenant: tenant.name
  }));
  
  return (
    <div>
      <h1>Tenant Module Assignments</h1>
      <DataGrid rows={rows} columns={columns} autoHeight />
    </div>
  );
};
```

---

**Last Updated:** 9. listopadu 2025
