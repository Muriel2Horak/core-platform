# ADM-005: Connector Management UI

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🟢 LOW  
**Dependencies:** MOD-005  
**Category:** Admin UI

---

## 📖 User Story

**As a tenant admin**,  
I want to configure integration connectors,  
So that modules can send emails, sync with Jira, etc.

---

## 🎯 Acceptance Criteria

- ⏳ Connector list (Email, Jira, M365, Slack, SMS)
- ⏳ Add/Edit connector form
- ⏳ Test connection button (verify credentials)
- ⏳ Credentials encrypted before save
- ⏳ Delete connector (confirm if used by modules)

---

## 💻 Implementation

### Connector Form

```tsx
// ConnectorForm.tsx

import React, { useState } from 'react';
import { TextField, Button, MenuItem } from '@mui/material';
import { connectorApi } from '@/api/connectors';

export const ConnectorForm: React.FC = () => {
  const [type, setType] = useState('email');
  const [config, setConfig] = useState({});
  
  const handleTest = async () => {
    const result = await connectorApi.test(type, config);
    if (result.success) {
      alert('✅ Connection successful!');
    } else {
      alert('❌ Connection failed: ' + result.error);
    }
  };
  
  const handleSave = async () => {
    await connectorApi.save(type, config);
    alert('Connector saved!');
  };
  
  return (
    <div>
      <h2>Configure Connector</h2>
      
      <TextField
        select
        label="Connector Type"
        value={type}
        onChange={e => setType(e.target.value)}
      >
        <MenuItem value="email">Email (SMTP)</MenuItem>
        <MenuItem value="jira">Jira</MenuItem>
        <MenuItem value="m365">Microsoft 365</MenuItem>
        <MenuItem value="slack">Slack</MenuItem>
      </TextField>
      
      {type === 'email' && (
        <>
          <TextField label="SMTP Host" />
          <TextField label="SMTP Port" type="number" />
          <TextField label="Username" />
          <TextField label="Password" type="password" />
        </>
      )}
      
      {type === 'jira' && (
        <>
          <TextField label="Jira Base URL" />
          <TextField label="API Token" type="password" />
          <TextField label="Project Key" />
        </>
      )}
      
      <Button onClick={handleTest}>Test Connection</Button>
      <Button variant="contained" onClick={handleSave}>Save</Button>
    </div>
  );
};
```

---

**Last Updated:** 9. listopadu 2025
