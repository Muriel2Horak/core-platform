# Frontend AI Integration Guide

This guide explains how to integrate AI help features into frontend pages.

## 🎯 Overview

The AI Help Widget provides contextual help for users based on metadata (META_ONLY mode). It displays:
- Current page structure and purpose
- Available fields with PII markers
- Possible actions with step-by-step guides
- Validation rules
- Workflow state information

## 📦 Components

### AiHelpWidget

Main component that displays AI-powered help.

**Location:** `frontend/src/components/AiHelpWidget.tsx`

**Props:**
- `routeId: string` - Route identifier (format: `entity.viewKind`, e.g., `users.list`)
- `visible?: boolean` - Show/hide widget (default: `true`)

**Features:**
- Automatically checks if AI is enabled (`/api/admin/ai/status`)
- Fetches context from `/api/ai/context?routeId={routeId}`
- Only renders when `AI_ENABLED=true`
- Shows structured help in expandable sections
- Displays "Probíhá aktualizace" warning when `state.updating=true`
- META_ONLY mode - no data values displayed

## 🚀 Quick Start

### 1. Import Component

```tsx
import { AiHelpWidget } from '../components/AiHelpWidget';
```

### 2. Define Route ID

```tsx
const routeId = 'users.list'; // Format: entity.viewKind
```

### 3. Add to Page

```tsx
export const UsersListPage = () => {
  const routeId = 'users.list';

  return (
    <Box data-route-id={routeId}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h4">Users</Typography>
        <AiHelpWidget routeId={routeId} />
      </Box>
      
      {/* Page content */}
    </Box>
  );
};
```

## 📋 Integration Checklist

- [ ] Import `AiHelpWidget` component
- [ ] Define `routeId` in format `entity.viewKind`
- [ ] Add `data-route-id` attribute to page container
- [ ] Add `<AiHelpWidget routeId={routeId} />` to page header/toolbar
- [ ] Verify metamodel has AI configuration for the entity
- [ ] Test with AI enabled and disabled
- [ ] Verify META_ONLY mode (no data values shown)

## 🎨 Common Patterns

### Header Integration

```tsx
<Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
  <Typography variant="h4">Page Title</Typography>
  <AiHelpWidget routeId="entity.viewKind" />
</Box>
```

### Toolbar Integration

```tsx
<Toolbar>
  <Typography variant="h6" sx={{ flexGrow: 1 }}>
    Page Title
  </Typography>
  <AiHelpWidget routeId="entity.viewKind" />
</Toolbar>
```

### Conditional Display

```tsx
<AiHelpWidget 
  routeId="entity.viewKind" 
  visible={user?.roles?.includes('ADVANCED_USER')} 
/>
```

## 🔧 Metamodel Requirements

For the widget to work properly, the entity must have AI configuration in metamodel YAML:

```yaml
# metamodel/entity.yaml
entity: User
table: users_directory

ai:
  visibility: META_ONLY
  policies:
    redactFields: [password, secret]

fields:
  - name: email
    type: email
    required: true
    pii: true           # Marked as PII
    helpSafe: false     # Not safe for help
    mask: "u***@d***.cz"  # Mask pattern
  
  - name: username
    type: string
    required: true
    pii: false
    helpSafe: true      # Safe for help

states:
  - code: active
    label: Active
    help: "User is active and can log in"

transitions:
  - code: deactivate
    from: active
    to: inactive
    label: Deactivate User
    icon: "🔒"
    dangerous: true
    help: "Deactivate user account"
    howto:
      - "Navigate to user detail"
      - "Click Deactivate button"
      - "Confirm action in dialog"
    preconditions:
      - "User must be in active state"
      - "No active sessions"
    postconditions:
      - "User cannot log in"
      - "All sessions terminated"
```

## 🧪 Testing

### Manual Testing

1. **Enable AI:**
   - Navigate to `Admin > Metamodel Studio > AI Config`
   - Toggle "AI Enabled" to ON
   - Save configuration

2. **Test Widget:**
   - Navigate to page with integrated widget
   - Verify "Nápověda" button appears
   - Click button to open help dialog
   - Verify structured help is displayed

3. **Verify META_ONLY:**
   - Check that no data values are shown
   - Only metadata (field names, types, labels) should appear
   - No user data, no record values

4. **Test AI Disabled:**
   - Navigate to `Admin > Metamodel Studio > AI Config`
   - Toggle "AI Enabled" to OFF
   - Verify widget disappears from page

### Unit Testing

```tsx
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { AiHelpWidget } from './AiHelpWidget';

it('should render help button when AI is enabled', async () => {
  global.fetch = vi.fn()
    .mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

  render(<AiHelpWidget routeId="test.route" />);

  await waitFor(() => {
    expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
  });
});
```

### E2E Testing (Playwright)

```typescript
test('AI help widget displays context', async ({ page }) => {
  // Enable AI in admin
  await page.goto('/admin/metamodel-studio');
  await page.click('text=AI Config');
  await page.click('[data-testid="ai-enabled-toggle"]');
  await page.click('text=Save');

  // Navigate to page with widget
  await page.goto('/admin/users');
  
  // Verify widget appears
  await expect(page.locator('[data-testid="ai-help-button"]')).toBeVisible();
  
  // Open help dialog
  await page.click('[data-testid="ai-help-button"]');
  
  // Verify context is displayed
  await expect(page.locator('[data-testid="ai-help-dialog"]')).toBeVisible();
  await expect(page.locator('text=META_ONLY')).toBeVisible();
});
```

## 🔍 Troubleshooting

### Widget Not Appearing

**Possible causes:**
1. AI is disabled globally (`AI_ENABLED=false`)
2. `visible` prop is set to `false`
3. `/api/admin/ai/status` endpoint returns error

**Solution:**
- Check AI config in Metamodel Studio
- Verify `/api/admin/ai/status` returns `{ enabled: true }`
- Check browser console for errors

### Empty Help Dialog

**Possible causes:**
1. RouteId doesn't match any entity in metamodel
2. Entity has no AI configuration
3. `/api/ai/context` returns 404

**Solution:**
- Verify `routeId` format: `entity.viewKind`
- Check entity YAML has AI section
- Test endpoint: `curl "http://localhost:8080/api/ai/context?routeId=users.list"`

### Error: "AI funkce nejsou dostupné"

**Possible causes:**
1. AI is disabled globally
2. Kill-switch is active

**Solution:**
- Enable AI in Metamodel Studio > AI Config tab
- Verify global-config.yaml has `ai.enabled: true`

### Error: "Stránka je momentálně uzamčena"

**Possible causes:**
1. Entity is in strict read mode (423 response)
2. Workflow state has locked=true

**Solution:**
- This is expected behavior for strict reads
- User should wait for update to complete
- Check workflow state in metamodel

## 📚 API Reference

### GET /api/admin/ai/status

Returns AI global status.

**Response:**
```json
{
  "enabled": true,
  "mode": "META_ONLY",
  "status": "active"
}
```

### GET /api/ai/context?routeId={routeId}

Returns AI context for the route.

**Response:**
```json
{
  "route": {
    "routeId": "users.list",
    "viewKind": "list",
    "entity": "User",
    "title": "Users List"
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
  "actions": [
    {
      "code": "create",
      "label": "Create User",
      "help": "Create a new user account",
      "howto": ["Step 1", "Step 2", "Step 3"],
      "preconditions": ["Admin role required"],
      "postconditions": ["New user created"]
    }
  ],
  "validations": [
    {
      "field": "email",
      "rule": "required",
      "message": "Email is required"
    }
  ],
  "state": {
    "current": "draft",
    "updating": false
  }
}
```

## 🔗 Related Documentation

- [AI Guide](../docs/AI_GUIDE.md) - Complete AI hooks architecture
- [AI Screen Checklist](../docs/AI_SCREEN_CHECKLIST.md) - Checklist for new screens
- [Streaming README](../STREAMING_README.md#-ai-hooks-meta_only) - AI integration with streaming

---

**Last Updated:** 2025-10-15  
**Version:** 1.0.0
