# INF-002: Module Audit Log

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** -  
**Category:** Infrastructure

---

## 📖 User Story

**As a platform admin**,  
I want audit logs of all module operations,  
So that I can track who installed/enabled/disabled modules and when.

---

## 🎯 Acceptance Criteria

- ⏳ Log all module operations (install, enable, disable, uninstall)
- ⏳ Log license operations (upload, validation, expiry)
- ⏳ Include: timestamp, user, action, module ID, tenant ID, result
- ⏳ Store in database (retention: 1 year)
- ⏳ UI viewer (filter by user, module, date range)
- ⏳ Export to CSV

---

## 💻 Implementation

### Database Schema

```sql
CREATE TABLE module_audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT NOW(),
    user_id VARCHAR(100),
    user_email VARCHAR(200),
    action VARCHAR(50) NOT NULL,          -- "MODULE_INSTALLED", "MODULE_ENABLED"
    module_id VARCHAR(100),
    tenant_id VARCHAR(100),
    result VARCHAR(20) NOT NULL,          -- "SUCCESS", "FAILED"
    error_message TEXT,
    metadata JSONB,                       -- Additional context
    
    CHECK (action IN ('MODULE_INSTALLED', 'MODULE_ENABLED', 'MODULE_DISABLED', 
                      'MODULE_UNINSTALLED', 'LICENSE_UPLOADED', 'LICENSE_VALIDATED',
                      'LICENSE_DENIED'))
);

CREATE INDEX idx_module_audit_timestamp ON module_audit_log(timestamp DESC);
CREATE INDEX idx_module_audit_user ON module_audit_log(user_id);
CREATE INDEX idx_module_audit_module ON module_audit_log(module_id);
```

---

## 📝 Logger Service

```java
@Service
public class ModuleAuditLogger {
    
    private final JdbcTemplate jdbcTemplate;
    private final UserContextHolder userContext;
    
    public void logModuleInstalled(String moduleId, String userId) {
        log("MODULE_INSTALLED", moduleId, null, userId, "SUCCESS", null, null);
    }
    
    public void logModuleEnabled(String moduleId, String userId) {
        log("MODULE_ENABLED", moduleId, null, userId, "SUCCESS", null, null);
    }
    
    public void logModuleDisabled(String moduleId, String userId) {
        log("MODULE_DISABLED", moduleId, null, userId, "SUCCESS", null, null);
    }
    
    public void logLicenseUploaded(String moduleId, String tenantId, String userId) {
        log("LICENSE_UPLOADED", moduleId, tenantId, userId, "SUCCESS", null, null);
    }
    
    public void logLicenseDenied(String moduleId, String tenantId, String reason) {
        log("LICENSE_DENIED", moduleId, tenantId, 
            userContext.getCurrentUserId(), "FAILED", reason, null);
    }
    
    private void log(String action, String moduleId, String tenantId, 
                     String userId, String result, String errorMessage, 
                     Map<String, Object> metadata) {
        
        String sql = """
            INSERT INTO module_audit_log 
            (action, module_id, tenant_id, user_id, user_email, result, error_message, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        """;
        
        String userEmail = userContext.getCurrentUserEmail();
        String metadataJson = metadata != null ? new ObjectMapper().writeValueAsString(metadata) : null;
        
        jdbcTemplate.update(sql, action, moduleId, tenantId, userId, userEmail, 
                           result, errorMessage, metadataJson);
    }
}
```

---

## 📊 Audit Viewer UI

```tsx
// AuditLogViewer.tsx

import React, { useState, useEffect } from 'react';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { Chip } from '@mui/material';
import { auditApi } from '@/api/audit';

export const AuditLogViewer: React.FC = () => {
  const [logs, setLogs] = useState([]);
  
  useEffect(() => {
    loadLogs();
  }, []);
  
  const loadLogs = async () => {
    const data = await auditApi.getModuleLogs();
    setLogs(data);
  };
  
  const columns: GridColDef[] = [
    { field: 'timestamp', headerName: 'Timestamp', width: 180 },
    { field: 'action', headerName: 'Action', width: 200 },
    { field: 'moduleId', headerName: 'Module', width: 150 },
    { field: 'userEmail', headerName: 'User', width: 200 },
    { 
      field: 'result', 
      headerName: 'Result', 
      width: 100,
      renderCell: (params) => (
        <Chip 
          label={params.value} 
          color={params.value === 'SUCCESS' ? 'success' : 'error'}
          size="small"
        />
      )
    },
    { field: 'errorMessage', headerName: 'Error', width: 300 }
  ];
  
  return (
    <div>
      <h1>Module Audit Log</h1>
      <DataGrid 
        rows={logs} 
        columns={columns} 
        pageSize={50}
        autoHeight
      />
    </div>
  );
};
```

---

**Last Updated:** 9. listopadu 2025
