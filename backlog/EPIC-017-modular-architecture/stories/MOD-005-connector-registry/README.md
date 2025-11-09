# MOD-005: Connector Registry

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** -  
**Category:** Module System

---

## 📖 User Story

**As a module**,  
I want to register integration connectors (Jira, M365, Slack),  
So that multiple modules can reuse connectors without duplication.

---

## 🎯 Acceptance Criteria

- ⏳ Global connector registry (Email, Jira, M365, Slack, SMS)
- ⏳ Modules reference connectors by ID
- ⏳ Connector credentials encrypted (AES-256)
- ⏳ Admin UI to configure connectors per tenant
- ⏳ Connection testing (verify credentials before save)

---

## 💻 Implementation

### Connector Manifest

```json
{
  "id": "helpdesk",
  "provides": {
    "connectors": [
      {
        "type": "email",
        "purpose": "Ticket notifications",
        "requiredConfig": ["smtp_host", "smtp_port", "username", "password"]
      },
      {
        "type": "jira",
        "purpose": "Sync tickets to Jira",
        "requiredConfig": ["base_url", "api_token", "project_key"]
      }
    ]
  }
}
```

### Database Schema

```sql
CREATE TABLE connectors (
    id BIGSERIAL PRIMARY KEY,
    connector_type VARCHAR(50) NOT NULL,    -- "email", "jira", "m365"
    tenant_id VARCHAR(100),
    config_encrypted BYTEA NOT NULL,        -- AES-256 encrypted JSON
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

**Last Updated:** 9. listopadu 2025
