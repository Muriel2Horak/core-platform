# AI Guide - In-App Agent Hooks (META_ONLY)

## Overview

AI hooks poskytují in-app AI agentům metadata o aplikaci bez vystavení skutečných dat.
Výchozí režim: **META_ONLY** - pouze metadata, žádné hodnoty.

**Status:** ✅ Implemented (Phase 1 - META_ONLY)  
**Version:** 1.0.0  
**Date:** 2025-10-14

---

## Table of Contents

1. [Architecture](#architecture)
2. [AIX Contract](#aix-contract)
3. [Workflow Annotations](#workflow-annotations)
4. [GUI Annotations](#gui-annotations)
5. [Context Assembly](#context-assembly)
6. [MCP Tools](#mcp-tools)
7. [Telemetry](#telemetry)
8. [Security](#security)
9. [Developer Guide](#developer-guide)
10. [Troubleshooting](#troubleshooting)

---

## Architecture

```
┌─────────────┐
│   AI Agent  │
│  (External) │
└──────┬──────┘
       │ HTTP/MCP
       ▼
┌─────────────────────────────────────┐
│   API Layer                         │
│  /api/ai/context                    │
│  /api/ai/mcp/ui_context/*          │
│  /api/ai/mcp/wf_context/*          │
│  /api/ai/mcp/auth/*                │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│   Context Assembler                 │
│   - Enforces META_ONLY              │
│   - Validates policies              │
│   - Assembles UI + WF + Auth        │
└──────┬──────────────────────────────┘
       │
   ┌───┴────┬──────────┬─────────┐
   ▼        ▼          ▼         ▼
┌─────┐ ┌──────┐ ┌────────┐ ┌──────┐
│ UI  │ │  WF  │ │  Auth  │ │ Data │
│ Ctx │ │  Ctx │ │  Ctx   │ │ (501)│
└─────┘ └──────┘ └────────┘ └──────┘
```

---

## AIX Contract

### Global Configuration

File: `backend/src/main/resources/metamodel/global-config.yaml`

```yaml
ai:
  enabled: false  # Master kill-switch
  mode: META_ONLY  # META_ONLY|REDACTED|FULL
  
  policies:
    defaultVisibility: META_ONLY
    redactFields: 
      - email
      - phone
      - nationalId
      - secret*
      - password*
    maxFields: 30
    maxRecords: 20
    maxTokens: 8000
  
  prompts:
    userAgent:
      system: "You are a helpful assistant..."
    devAgent:
      system: "You are a developer assistant..."
  
  tools:
    - name: ui_context.get_current_view
    - name: wf_context.get_workflow
    - name: auth.get_user_capabilities
    - name: data_context.query  # stub
```

### Entity-Level AI Config

```yaml
entity: User
table: users_directory

# AI configuration (optional)
ai:
  policies:
    # Override global policies for this entity
    maxFields: 20
  
  # Entity-specific help
  prompts:
    userAgent:
      system: "Help text for User entity..."
```

### Field-Level Annotations

```yaml
fields:
  - name: email
    type: email
    required: true
    # AI annotations
    pii: true           # Contains PII, will be masked
    helpSafe: false     # Don't expose even in help mode
    mask: "u***@d***.cz"  # Mask pattern for REDACTED mode
  
  - name: username
    type: string
    required: true
    pii: false
    helpSafe: true     # Safe to expose in help
```

---

## Workflow Annotations

### States

```yaml
states:
  - code: draft        # Stable identifier
    label: Draft       # Human-readable
    help: "Initial state for new proposals"
```

### Actions/Transitions

```yaml
transitions:
  - code: submit_for_review
    from: draft
    to: review
    label: Submit for Review
    help: "Submit proposal for review by authorized users"
    icon: "send"
    dangerous: false
    
    # AI guidance
    preconditions:
      - "Proposal must be complete"
      - "User must have submit permission"
    
    postconditions:
      - "Proposal moves to review state"
      - "Reviewer is notified"
    
    sideEffects:
      - "Email notification sent"
      - "Audit log created"
    
    errors:
      - "Incomplete proposal"
      - "Insufficient permissions"
    
    # How-to steps (3-7 recommended)
    howto:
      - "Open proposal in edit mode"
      - "Review all required fields"
      - "Click 'Submit for Review' button"
      - "Confirm submission"
      - "Wait for reviewer notification"
    
    streamingPriority: HIGH
```

---

## GUI Annotations

### Route Structure

Routes follow pattern: `{entity}.{viewKind}`

- `users.list` → User List
- `users.detail` → User Detail
- `users.edit` → User Edit
- `proposals.review` → Proposal Review (custom)

### View Kinds

- `list` - Table/grid view
- `detail` - Read-only detail
- `edit` - Edit form
- `create` - Creation wizard
- `wizard` - Multi-step process

### UI Elements

All UI elements should have:
- `controlId` - Stable identifier
- `data-testid` - For E2E tests
- `aria-label` - Accessibility
- Localized `label` via i18n

---

## Context Assembly

### GET /api/ai/context?routeId={route}

Returns META_ONLY context:

```json
{
  "screen": {
    "title": "User Detail",
    "routeId": "users.detail",
    "entity": "User",
    "viewKind": "detail",
    "widgets": ["form", "actions", "relatedEntities"]
  },
  "fields": [
    {
      "name": "email",
      "type": "email",
      "label": "Email",
      "required": true,
      "pii": true,
      "helpSafe": false
    }
  ],
  "validations": [
    {
      "field": "email",
      "rule": "required",
      "message": "Email is required"
    }
  ],
  "workflow": {
    "entity": "User",
    "states": [...],
    "actions": [...],
    "howto": {...}
  },
  "auth": {
    "authenticated": true,
    "canView": true,
    "canEdit": false,
    "canExecute": []
  },
  "metadata": {
    "tenantId": "...",
    "userId": "user****",  // Masked
    "correlationId": "...",
    "timestamp": 1728928800000,
    "policy": {
      "visibility": "META_ONLY",
      "maxFields": 30,
      "maxRecords": 20
    }
  }
}
```

**Critical:** META_ONLY mode NEVER returns:
- `value` fields
- `rows` arrays
- `data` objects
- Any actual user/business data

---

## MCP Tools

### ui_context.get_current_view

```http
POST /api/ai/mcp/ui_context/get_current_view
Content-Type: application/json

{
  "routeId": "users.detail"
}
```

Returns: UI metadata (screen, fields, widgets, validations)

### wf_context.get_workflow

```http
POST /api/ai/mcp/wf_context/get_workflow
Content-Type: application/json

{
  "entity": "Proposal"
}
// or
{
  "routeId": "proposals.review"
}
```

Returns: Workflow metadata (states, actions, how-to steps)

### auth.get_user_capabilities

```http
POST /api/ai/mcp/auth/get_user_capabilities
Content-Type: application/json

{
  "userId": "...",
  "routeId": "users.detail"
}
```

Returns: User capabilities (canView, canEdit, canExecute) - STUB

### data_context.query

```http
POST /api/ai/mcp/data_context/query
Content-Type: application/json

{
  "routeId": "users.list",
  "fields": ["username", "email"],
  "filters": {...},
  "mode": "META_ONLY"
}
```

Returns: 501 NOT_IMPLEMENTED (stub for future)

---

## Telemetry

### Prometheus Metrics

```promql
# Request rate by route and mode
rate(ai_requests_total{route="users.detail",mode="META_ONLY"}[5m])

# Error rate
rate(ai_errors_total{type="INVALID_ROUTE"}[5m])

# MCP tool usage
sum by(tool) (increase(mcp_calls_total[24h]))

# Help requests
increase(ai_help_requests_total{route="users.detail"}[1h])

# Policy denials
increase(ai_policy_denied_total[1h])

# Tokens (stub, always 0)
sum(ai_tokens_total{type="input"})
```

### Grafana Dashboards

- **AI Overview** (`ai-overview`): Requests/day, error rate, route breakdown
- **AI Ops** (`ai-ops`): MCP calls, kill-switch status, policy denials

---

## Security

### PII Protection

Fields marked with `pii: true`:
- **META_ONLY**: Field metadata exposed, NO values
- **REDACTED**: Masked values (`u***@d***.cz`)
- **FULL**: Real values (requires explicit permission + audit)

### Redact Patterns

Global patterns in `ai.policies.redactFields`:
- Exact match: `email`, `phone`
- Wildcard: `secret*`, `password*`, `token*`

### Kill-Switch

```yaml
ai:
  enabled: false  # Disable all AI endpoints
```

When `enabled: false`:
- `/api/ai/context` → 404
- `/api/ai/mcp/*` → 404
- Help widget hidden in UI

### RBAC

- **PlatformAdmin/Ops**: Full AI config write access
- **Tenant Admin**: Read-only AI config
- **Users**: Can use help widget (if enabled)

---

## Developer Guide

### Adding New Entity AI Support

1. **Mark PII fields** in YAML:
   ```yaml
   fields:
     - name: nationalId
       type: string
       pii: true
       helpSafe: false
   ```

2. **Add workflow how-to** for critical actions:
   ```yaml
   transitions:
     - code: approve
       howto:
         - "Open proposal in review mode"
         - "Check all validation criteria"
         - "Click Approve button"
   ```

3. **Annotate routes** in frontend:
   ```tsx
   <Route 
     id="proposals.review"
     data-testid="proposals-review"
     aria-label="Proposal Review Screen"
   />
   ```

4. **Test META_ONLY**:
   ```bash
   curl -X GET "http://localhost:8080/api/ai/context?routeId=proposals.review" \
     | jq '.fields[] | select(.name == "nationalId") | has("value")'
   # Should return: false
   ```

### Checklist for New Screens

- [ ] routeId defined
- [ ] viewKind specified (list|detail|edit|wizard)
- [ ] All fields have `name`, `type`, `label`
- [ ] PII fields marked with `pii: true`
- [ ] Help-safe fields marked with `helpSafe: true`
- [ ] Workflow actions have `howto` steps (3-7)
- [ ] i18n labels for all UI text
- [ ] data-testid for E2E tests

---

## Troubleshooting

### AI Context Returns 404

**Cause:** AI is disabled  
**Fix:** Set `ai.enabled: true` in `global-config.yaml`

### AI Context Returns Empty Fields

**Cause:** Entity not found or invalid routeId  
**Fix:** Check entity name and route mapping

### META_ONLY Validation Fails

**Cause:** Response contains `value` or `rows` fields  
**Fix:** Review ContextAssembler - ensure no data leakage

### MCP Tool Returns 501

**Cause:** Tool not implemented yet (e.g., `data_context.query`)  
**Fix:** Expected for stubs - implement when needed

### Metrics Not Appearing

**Cause:** Prometheus not scraping or AI not used  
**Fix:** Check Prometheus targets, make test AI requests

---

## References

- Metamodel: `backend/src/main/resources/metamodel/`
- AI Services: `backend/src/main/java/cz/muriel/core/service/ai/`
- Tests: `backend/src/test/java/cz/muriel/core/.../ai/`
- Dashboards: `docker/grafana/provisioning/dashboards/ai-*.json`

---

## Next Steps (Future)

1. **REDACTED mode**: Implement field masking
2. **FULL mode**: Add audit logging and RBAC checks
3. **data_context.query**: Implement schema-only queries
4. **Help widget**: Frontend implementation
5. **AI tokens**: Track actual token usage when model integrated
6. **Kafka telemetry**: Publish ai.telemetry events

---

**Version:** 1.0.0 (META_ONLY phase)  
**Last Updated:** 2025-10-14
