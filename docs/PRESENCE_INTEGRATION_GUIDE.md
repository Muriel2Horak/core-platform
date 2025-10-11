# Presence System - Integration Guide

**Author:** Martin Horák  
**Created:** 2025-10-11  
**Last Updated:** 2025-10-11  
**Version:** 1.0

---

## 📋 Overview

This guide demonstrates how to integrate the **S2 Real-Time Presence System** into any edit page in the Core Platform. The presence system provides:

- **Real-time user tracking:** See who else is viewing the same entity
- **Stale mode warnings:** Know when another user is actively editing
- **Field-level locking:** Track which fields are being edited
- **Connection states:** Loading, reconnecting, offline indicators
- **UX feedback:** Snackbar notifications for all state changes

---

## 🎯 Quick Start

### 1. Import Presence Hooks & Components

```tsx
import { usePresence } from '../../lib/presence/usePresence';
import { PresenceIndicator } from '../../components/presence/PresenceIndicator';
```

### 2. Initialize Presence Hook

```tsx
const { presence, acquireLock, releaseLock, error: presenceError } = usePresence(
  {
    entity: 'Order',        // Entity type (e.g., User, Tenant, Role, Order)
    id: orderId || '',      // Entity ID
    tenantId: currentUser?.tenantKey || '',
    userId: currentUser?.id || '',
  },
  {
    enabled: !!orderId && !!currentUser?.id,
    wsUrl: 'ws://localhost:8080/ws/presence',
  }
);
```

### 3. Display Presence Indicator

```tsx
<Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
  <Typography variant="h5">Edit Order #{orderId}</Typography>
  <PresenceIndicator presence={presence} currentUserId={currentUser?.id} />
</Box>
```

### 4. Show Stale Mode Warning

```tsx
{presence.stale && (
  <Alert severity="warning" sx={{ mb: 2 }} data-testid="stale-warning">
    ⚠️ This order is currently being modified by <strong>{presence.busyBy}</strong>. 
    Your changes may conflict.
  </Alert>
)}
```

### 5. Disable Form in Stale Mode

```tsx
<TextField
  fullWidth
  label="Order Total"
  value={order.total}
  onChange={(e) => handleFieldChange('total', e.target.value)}
  onFocus={() => acquireLock('total')}
  onBlur={() => releaseLock('total')}
  disabled={presence.stale}  // ← Disable when stale
  margin="normal"
/>
```

---

## 📚 Full Integration Pattern

### Complete Example: `OrderEditPage.tsx`

```tsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
} from '@mui/material';
import { Save, Cancel } from '@mui/icons-material';
import axios from 'axios';
import { usePresence } from '../../lib/presence/usePresence';
import { PresenceIndicator } from '../../components/presence/PresenceIndicator';

interface Order {
  id: string;
  customerId: string;
  total: number;
  status: string;
}

export const OrderEditPage = ({ currentUser }: { currentUser: any }) => {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveTimer, setSaveTimer] = useState<NodeJS.Timeout | null>(null);

  // S2: Real-time presence tracking
  const { presence, acquireLock, releaseLock, error: presenceError } = usePresence(
    {
      entity: 'Order',
      id: orderId || '',
      tenantId: currentUser?.tenantKey || '',
      userId: currentUser?.id || '',
    },
    {
      enabled: !!orderId && !!currentUser?.id,
      wsUrl: 'ws://localhost:8080/ws/presence',
    }
  );

  // Fetch order data
  useEffect(() => {
    const fetchOrder = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`/api/orders/${orderId}`);
        setOrder(response.data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch order');
      } finally {
        setLoading(false);
      }
    };

    if (orderId) {
      fetchOrder();
    }
  }, [orderId]);

  // Cleanup save timer on unmount
  useEffect(() => {
    return () => {
      if (saveTimer) {
        clearTimeout(saveTimer);
      }
    };
  }, [saveTimer]);

  const handleFieldFocus = (fieldName: string) => {
    acquireLock(fieldName);
  };

  const handleFieldBlur = async (fieldName: string) => {
    releaseLock(fieldName);

    // Auto-save on blur (debounced 500ms)
    if (saveTimer) {
      clearTimeout(saveTimer);
    }

    const timer = setTimeout(async () => {
      await handleSave(false); // Silent save
    }, 500);

    setSaveTimer(timer);
  };

  const handleFieldChange = (fieldName: keyof Order, value: any) => {
    if (order) {
      setOrder({ ...order, [fieldName]: value });
    }
  };

  const handleSave = async (showSnackbar = true) => {
    if (!order) return;

    try {
      setSaving(true);
      await axios.put(`/api/orders/${orderId}`, order);
      // Success notification handled by future WithPresenceFeedback HOC
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save order');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    navigate('/orders');
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error && !order) {
    return (
      <Box p={3}>
        <Alert severity="error">{error}</Alert>
        <Button variant="outlined" onClick={handleCancel} sx={{ mt: 2 }}>
          Back to Orders
        </Button>
      </Box>
    );
  }

  if (!order) {
    return (
      <Box p={3}>
        <Alert severity="warning">Order not found</Alert>
        <Button variant="outlined" onClick={handleCancel} sx={{ mt: 2 }}>
          Back to Orders
        </Button>
      </Box>
    );
  }

  const isStale = presence.stale;
  const busyBy = presence.busyBy;

  return (
    <Box p={3}>
      {/* Header with Presence Indicator */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5">Edit Order #{orderId}</Typography>
        <PresenceIndicator presence={presence} currentUserId={currentUser?.id} />
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Presence Error */}
      {presenceError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Presence tracking unavailable: {presenceError}
        </Alert>
      )}

      {/* Stale Mode Warning */}
      {isStale && (
        <Alert severity="warning" sx={{ mb: 2 }} data-testid="stale-warning">
          ⚠️ This order is currently being modified by <strong>{busyBy}</strong>. 
          Your changes may conflict.
        </Alert>
      )}

      <Paper elevation={2} sx={{ p: 3 }}>
        {/* Form Fields */}
        <Box component="form" noValidate sx={{ mt: 2 }}>
          <TextField
            fullWidth
            label="Customer ID"
            value={order.customerId}
            onChange={(e) => handleFieldChange('customerId', e.target.value)}
            onFocus={() => handleFieldFocus('customerId')}
            onBlur={() => handleFieldBlur('customerId')}
            disabled={isStale}
            margin="normal"
            data-testid="field-customerId"
          />

          <TextField
            fullWidth
            label="Total"
            type="number"
            value={order.total}
            onChange={(e) => handleFieldChange('total', parseFloat(e.target.value))}
            onFocus={() => handleFieldFocus('total')}
            onBlur={() => handleFieldBlur('total')}
            disabled={isStale}
            margin="normal"
            data-testid="field-total"
          />

          {/* Action Buttons */}
          <Box display="flex" gap={2} mt={3}>
            <Button
              variant="contained"
              color="primary"
              startIcon={saving ? <CircularProgress size={20} /> : <Save />}
              onClick={() => handleSave(true)}
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
            <Button
              variant="outlined"
              startIcon={<Cancel />}
              onClick={handleCancel}
              disabled={saving}
            >
              Cancel
            </Button>
          </Box>
        </Box>
      </Paper>
    </Box>
  );
};
```

---

## 🔑 Key Integration Points

### 1. **PresenceState Interface**

```tsx
interface PresenceState {
  users: string[];          // Active user IDs
  stale: boolean;           // Another user is editing
  busyBy: string | null;    // User ID who triggered stale mode
  version: number | null;   // Entity version
  connected: boolean;       // WebSocket connected
  loading: boolean;         // Initial connection loading
  reconnecting: boolean;    // Currently reconnecting
  reconnectAttempt: number; // Reconnection attempt (1-5)
}
```

### 2. **PresenceIndicator Props**

```tsx
interface PresenceIndicatorProps {
  presence: PresenceState;
  currentUserId: string;
  getUserDisplayName?: (userId: string) => string; // Optional
}
```

Shows:
- Connection status (green dot = online, gray = offline)
- Active user count + avatars (max 3 visible)
- Reconnecting badge (yellow with spinner)
- Stale mode badge (red)
- Entity version badge

### 3. **Lock Management Pattern**

```tsx
// Acquire lock on focus
const handleFieldFocus = (fieldName: string) => {
  acquireLock(fieldName);
};

// Release lock on blur + auto-save
const handleFieldBlur = async (fieldName: string) => {
  releaseLock(fieldName);
  
  // Debounced auto-save
  if (saveTimer) clearTimeout(saveTimer);
  const timer = setTimeout(async () => {
    await handleSave(false); // Silent save
  }, 500);
  setSaveTimer(timer);
};
```

**Important:**
- Always cleanup timer on unmount: `useEffect(() => () => clearTimeout(saveTimer), [saveTimer])`
- Use `disabled={presence.stale}` to prevent edits during stale mode
- Silent save (`showSnackbar=false`) on blur to avoid notification spam

### 4. **Auto-Save Triggers**

- **On blur:** Debounced 500ms (for text fields)
- **On change:** Immediate (for dropdowns, checkboxes, permission changes)

```tsx
const handleStatusChange = (event: SelectChangeEvent<string>) => {
  if (entity) {
    setEntity({ ...entity, status: event.target.value });
    // Immediate save
    setTimeout(() => handleSave(true), 100);
  }
};
```

---

## 🧪 Testing Checklist

### Manual Testing (2 Browsers)

- [ ] Open same entity in 2 browsers (different users)
- [ ] Verify both users appear in presence indicator
- [ ] Focus field in Browser A → verify lock acquired
- [ ] Try to edit same field in Browser B → should be disabled
- [ ] Change field in Browser A → verify stale warning appears in Browser B
- [ ] Disconnect WiFi → verify reconnecting badge appears
- [ ] Reconnect WiFi → verify reconnecting badge disappears
- [ ] Test auto-save on blur (wait 500ms after blur)
- [ ] Test immediate save on dropdown change

### Accessibility Testing

- [ ] Tab through all fields with keyboard
- [ ] Screen reader announces presence status
- [ ] Screen reader announces stale mode warnings
- [ ] ARIA labels on all interactive elements

---

## 🎨 UI Components Available

### 1. **PresenceIndicator**
Shows connection status, active users, stale mode, version.

### 2. **FieldLockIndicator** (Optional)
Shows lock icon on focused fields (green=own lock, red=other user's lock).

### 3. **PresenceErrorBoundary** (Future)
Wraps page to catch WebSocket errors gracefully.

### 4. **WithPresenceFeedback** (Future)
HOC wrapper adding snackbar notifications for all presence events.

### 5. **PresenceLoadingSkeleton**
Shows while WebSocket connecting.

### 6. **FormLoadingSkeleton**
Shows while form data loading.

---

## 🚀 Best Practices

### 1. **Always Check Stale Mode**
```tsx
disabled={presence.stale}
```

### 2. **Auto-Save Pattern**
- Debounce text fields (500ms)
- Immediate save for critical changes (status, permissions)

### 3. **Error Handling**
```tsx
{presenceError && (
  <Alert severity="warning">
    Presence tracking unavailable: {presenceError}
  </Alert>
)}
```

### 4. **Accessibility**
- Use `data-testid` for E2E tests
- Presence indicator has ARIA labels built-in
- Stale warnings use semantic HTML (`<strong>` for user names)

### 5. **Cleanup**
```tsx
useEffect(() => {
  return () => {
    if (saveTimer) clearTimeout(saveTimer);
  };
}, [saveTimer]);
```

---

## 📦 Implemented Examples

### 1. **UserEditPage** (`frontend/src/pages/admin/UserEditPage.tsx`)
- Fields: username, email, firstName, lastName, tenantKey, roles
- Auto-save on blur (500ms)
- Full presence integration with PresenceIndicator

### 2. **TenantEditPage** (`frontend/src/pages/admin/TenantEditPage.tsx`)
- Fields: name, slug, status, description
- Auto-save on blur (500ms)
- Immediate save on status change

### 3. **RoleEditPage** (`frontend/src/pages/admin/RoleEditPage.tsx`)
- Fields: name, description, permissions (multi-select)
- Auto-save on blur (500ms)
- Immediate save on permissions change

---

## 🔗 Routing

Add to `App.jsx`:

```jsx
<Route path="/core-admin/orders/:orderId/edit" 
  element={<OrderEditPage currentUser={user} />} 
/>
```

---

## 🐛 Troubleshooting

### WebSocket Connection Errors

**Symptom:** Presence indicator shows "Offline"

**Solutions:**
1. Check backend is running: `http://localhost:8080/actuator/health`
2. Check WebSocket endpoint: `ws://localhost:8080/ws/presence`
3. Check browser console for CORS errors
4. Verify Redis is running: `docker ps | grep redis`

### Stale Mode Not Working

**Symptom:** No stale warning when other user edits

**Solutions:**
1. Check Kafka is running: `docker ps | grep kafka`
2. Check backend logs for Kafka consumer errors
3. Verify both users in same tenant
4. Check entity type matches exactly (case-sensitive)

### Auto-Save Not Triggering

**Symptom:** Changes not saved after blur

**Solutions:**
1. Check saveTimer cleanup in useEffect
2. Verify 500ms debounce delay
3. Check backend API endpoint exists
4. Check browser console for axios errors

---

## 📊 Performance Considerations

### WebSocket Heartbeats
- Sent every 30 seconds
- Backend timeout: 60 seconds (2x heartbeat interval)
- Automatic reconnection with exponential backoff (1s, 2s, 4s, 8s, 16s)

### Redis TTL
- Presence records expire after 60 seconds
- Heartbeats refresh TTL
- Stale mode triggered on disconnect

### Auto-Save Debouncing
- 500ms delay prevents excessive API calls
- Immediate save for critical changes
- Silent save (no snackbar) on blur

---

## 🎯 Future Enhancements

### 1. **WithPresenceFeedback HOC**
Wrap entire page to add snackbar notifications:

```tsx
export const OrderEditPage = WithPresenceFeedback(OrderEditPageInner);
```

Shows toasts for:
- Connected/disconnected
- Stale mode activated/deactivated
- Lock acquired: "Editing {field}"

### 2. **PresenceErrorBoundary**
Wrap page to catch all presence errors:

```tsx
<PresenceErrorBoundary>
  <OrderEditPage currentUser={user} />
</PresenceErrorBoundary>
```

### 3. **Field-Level Lock UI**
Add lock indicator to each field:

```tsx
import { FieldLockIndicator } from '../../components/presence/FieldLockIndicator';

<TextField
  ...
  InputProps={{
    endAdornment: focusedField === 'total' && (
      <FieldLockIndicator
        fieldName="total"
        isLocked={!!presence.locks?.total}
        lockedBy={presence.locks?.total}
        currentUserId={currentUser?.id}
      />
    ),
  }}
/>
```

---

## ✅ Integration Checklist

- [ ] Import `usePresence` hook
- [ ] Import `PresenceIndicator` component
- [ ] Initialize presence hook with entity/id/tenantId/userId
- [ ] Display PresenceIndicator in page header
- [ ] Show stale mode warning alert
- [ ] Disable form when `presence.stale === true`
- [ ] Call `acquireLock(field)` on field focus
- [ ] Call `releaseLock(field)` on field blur
- [ ] Implement auto-save with 500ms debounce
- [ ] Cleanup timers on unmount
- [ ] Add `data-testid` attributes for E2E tests
- [ ] Add routing in `App.jsx`
- [ ] Test with 2 browsers (multi-user)

---

**Need help?** Check existing implementations in `UserEditPage`, `TenantEditPage`, or `RoleEditPage`.
