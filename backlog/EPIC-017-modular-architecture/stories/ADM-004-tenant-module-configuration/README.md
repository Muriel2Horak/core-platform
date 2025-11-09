# ADM-004: Tenant Module Configuration

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** MOD-004  
**Category:** Admin UI

---

## 📖 User Story

**As a tenant admin**,  
I want to configure my enabled modules,  
So that I can customize module behavior (e.g., SLA times, notification emails).

---

## 🎯 Acceptance Criteria

- ⏳ My Modules page (tenant view)
- ⏳ Per-module configuration form (dynamic from manifest)
- ⏳ Save configuration (stored in JSONB column)
- ⏳ Reset to defaults button
- ⏳ Configuration validation (required fields, formats)

---

## 💻 Implementation

### Configuration Form

```tsx
// ModuleConfiguration.tsx

import React, { useState } from 'react';
import { TextField, Button, Switch, FormControlLabel } from '@mui/material';
import { moduleApi } from '@/api/modules';

interface Props {
  moduleId: string;
  tenantId: string;
}

export const ModuleConfiguration: React.FC<Props> = ({ moduleId, tenantId }) => {
  const [config, setConfig] = useState({});
  
  const handleSave = async () => {
    await moduleApi.saveConfiguration(tenantId, moduleId, config);
    alert('Configuration saved!');
  };
  
  return (
    <div>
      <h2>Configure {moduleId}</h2>
      
      {/* Example: Helpdesk module config */}
      {moduleId === 'helpdesk' && (
        <>
          <TextField
            label="SLA Response Time (hours)"
            type="number"
            value={config.slaResponseHours || 4}
            onChange={e => setConfig({ ...config, slaResponseHours: e.target.value })}
          />
          
          <TextField
            label="Notification Email"
            type="email"
            value={config.notificationEmail || ''}
            onChange={e => setConfig({ ...config, notificationEmail: e.target.value })}
          />
          
          <FormControlLabel
            control={
              <Switch
                checked={config.enableAutomations || false}
                onChange={e => setConfig({ ...config, enableAutomations: e.target.checked })}
              />
            }
            label="Enable Automations"
          />
        </>
      )}
      
      <Button variant="contained" onClick={handleSave}>
        Save Configuration
      </Button>
    </div>
  );
};
```

---

**Last Updated:** 9. listopadu 2025
