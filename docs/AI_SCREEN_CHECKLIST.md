# AI-Ready Screen Checklist

Use this checklist when implementing new screens to ensure they are AI-agent friendly.

## 🎯 Route & Metadata

- [ ] `routeId` defined in format `{entity}.{viewKind}`
  - Examples: `users.list`, `proposals.review`, `workflow-draft.edit`
- [ ] `viewKind` specified: `list`, `detail`, `edit`, `create`, `wizard`
- [ ] `entity` mapped to metamodel entity name
- [ ] `title` localized via i18n

## 📋 Fields

- [ ] All fields have `name` (stable identifier)
- [ ] All fields have `type` (uuid, string, email, text, long, timestamp, etc.)
- [ ] All fields have `label` (human-readable, localized)
- [ ] PII fields marked with `pii: true` in metamodel
  - Examples: email, phone, nationalId, passport, taxId
- [ ] Help-safe fields marked with `helpSafe: true`
  - Safe to expose in AI help without revealing data
- [ ] Optional: `mask` pattern for REDACTED mode
  - Example: `"u***@d***.cz"` for emails

## 🔄 Workflow

- [ ] Entity has `states[]` defined if stateful
  - Each state has: `code` (stable ID), `label`, optional `help`
- [ ] Entity has `transitions[]` for actions
  - Each transition has: `code`, `from`, `to`, `label`
- [ ] Critical actions have `howto` steps (3-7 recommended)
  - Clear, actionable steps a user can follow
- [ ] Actions have `icon` for UI consistency
- [ ] Dangerous actions marked with `dangerous: true`
- [ ] Actions have `preconditions`, `postconditions`, `sideEffects` (optional but recommended)
- [ ] Streaming-enabled actions have `streamingPriority` (CRITICAL, HIGH, NORMAL, BULK)

## 🎨 UI Elements

- [ ] All controls have `controlId` (stable identifier)
  - Examples: `btn-submit`, `input-email`, `table-users`
- [ ] All controls have `data-testid` for E2E tests
  - Examples: `data-testid="submit-button"`
- [ ] All controls have `aria-label` for accessibility
- [ ] Buttons/actions have clear labels (localized)
- [ ] Forms have validation rules in metamodel
  - `required`, `maxLength`, `pattern`, etc.

## 🔒 Security & Permissions

- [ ] RBAC rules defined for route/actions
- [ ] PII fields protected in AI context
- [ ] Sensitive actions require confirmation
- [ ] Audit logging enabled for critical actions

## 🧪 Testing

- [ ] Unit tests for field validation
- [ ] Integration tests for workflow transitions
- [ ] E2E tests using `data-testid` attributes
- [ ] AI context endpoint tested:
  ```bash
  curl "http://localhost:8080/api/ai/context?routeId=your.route" | jq
  ```
- [ ] Verified META_ONLY mode (no `value` or `rows` fields)

## 📊 Monitoring

- [ ] Screen logged in usage metrics
- [ ] AI help requests tracked (if help widget added)
- [ ] Critical actions have telemetry

## 📚 Documentation

- [ ] Route documented in README or docs/
- [ ] Workflow diagram created (if complex)
- [ ] Help text written (if needed)
- [ ] i18n keys added for all labels

---

## Example: Users Detail Screen

```yaml
# metamodel/user.yaml
entity: User
table: users_directory

fields:
  - name: email
    type: email
    required: true
    pii: true           # ✅ PII marked
    helpSafe: false     # ✅ Not safe for help
    mask: "u***@d***.cz"  # ✅ Mask pattern
  
  - name: username
    type: string
    required: true
    pii: false
    helpSafe: true      # ✅ Safe for help
```

```tsx
// frontend/src/pages/Users/Detail.tsx
export function UserDetail() {
  return (
    <Box data-route-id="users.detail">  {/* ✅ routeId */}
      <Typography variant="h4" data-testid="page-title">
        {t('users.detail.title')}  {/* ✅ i18n */}
      </Typography>
      
      <TextField
        name="email"  {/* ✅ stable name */}
        data-testid="input-email"  {/* ✅ test ID */}
        aria-label={t('users.fields.email')}  {/* ✅ aria-label */}
        {...}
      />
      
      <Button
        data-testid="btn-edit"  {/* ✅ test ID */}
        aria-label={t('users.actions.edit')}
        {...}
      >
        {t('common.edit')}  {/* ✅ i18n */}
      </Button>
    </Box>
  );
}
```

```bash
# Test AI context
curl "http://localhost:8080/api/ai/context?routeId=users.detail" | jq '.fields[] | select(.name == "email")'

# Expected output:
{
  "name": "email",
  "type": "email",
  "label": "Email",
  "required": true,
  "pii": true,
  "helpSafe": false
  // NOTE: No "value" field in META_ONLY mode ✅
}
```

---

## Quick Commands

```bash
# Validate metamodel YAML
mvn test -Dtest=MetamodelValidatorTest

# Test AI context for new route
curl "http://localhost:8080/api/ai/context?routeId=your.new.route" | jq

# Test MCP workflow export
curl -X POST http://localhost:8080/api/ai/mcp/wf_context/get_workflow \
  -H "Content-Type: application/json" \
  -d '{"entity": "YourEntity"}' | jq

# Check AI metrics
curl http://localhost:8080/actuator/prometheus | grep "ai_requests_total"
```

---

**Last Updated:** 2025-10-14  
**See:** `docs/AI_GUIDE.md` for complete guide
