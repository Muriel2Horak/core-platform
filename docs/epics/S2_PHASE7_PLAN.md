# S2 Phase 7: UI Component Polish

**Status:** 🚧 In Progress  
**Estimate:** 2h  
**Started:** 11. října 2025

---

## 🎯 Objectives

Polish frontend presence components for production readiness:
1. Add loading states
2. Add error boundaries
3. Improve UX feedback
4. Handle edge cases
5. Add accessibility features

---

## ✅ Tasks

### 1. Loading States (0.5h)

**PresenceIndicator.tsx:**
- [ ] Skeleton loader while connecting
- [ ] Spinner during reconnection attempts
- [ ] "Reconnecting..." badge with attempt count
- [ ] Smooth transition from loading → connected

**UserEditPage.tsx:**
- [ ] Loading skeleton for form
- [ ] Disable form interactions while presence is connecting
- [ ] Show "Connecting to presence..." message

**Implementation:**
```tsx
// Add loading prop to PresenceIndicator
interface PresenceIndicatorProps {
  presence: PresenceState;
  loading?: boolean;  // NEW
  reconnecting?: boolean;  // NEW
  reconnectAttempt?: number;  // NEW
}

// Show skeleton when loading
{loading && <Skeleton variant="rectangular" width={200} height={32} />}

// Show reconnecting badge
{reconnecting && (
  <Chip 
    icon={<Refresh className="animate-spin" />}
    label={`Reconnecting (${reconnectAttempt}/5)...`}
    color="warning"
    size="small"
  />
)}
```

---

### 2. Error Boundaries (0.5h)

**Create PresenceErrorBoundary.tsx:**
- [ ] Catch WebSocket connection errors
- [ ] Catch React component errors
- [ ] Show fallback UI with retry button
- [ ] Log errors to console/monitoring

**Implementation:**
```tsx
// frontend/src/components/presence/PresenceErrorBoundary.tsx
export class PresenceErrorBoundary extends React.Component {
  state = { hasError: false, error: null };
  
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }
  
  componentDidCatch(error, errorInfo) {
    console.error('Presence error:', error, errorInfo);
    // TODO: Send to monitoring (Sentry, etc.)
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <Alert severity="error">
          <AlertTitle>Presence System Error</AlertTitle>
          <Typography variant="body2">
            Real-time presence tracking is temporarily unavailable.
          </Typography>
          <Button onClick={() => window.location.reload()}>
            Reload Page
          </Button>
        </Alert>
      );
    }
    return this.props.children;
  }
}
```

**Wrap UserEditPage:**
```tsx
<PresenceErrorBoundary>
  <UserEditPage />
</PresenceErrorBoundary>
```

---

### 3. UX Improvements (0.5h)

**Connection Status Feedback:**
- [ ] Toast notification on connect/disconnect
- [ ] Visual feedback when acquiring lock (success/failure)
- [ ] Tooltip hints for presence badges
- [ ] Animated transitions (fade in/out)

**Lock Acquisition Feedback:**
```tsx
// Show success toast when lock acquired
const handleFieldFocus = async (fieldName: string) => {
  setFocusedField(fieldName);
  const success = await acquireLock(fieldName);
  
  if (success) {
    // Show green checkmark animation
    setLockSuccess(fieldName);
    setTimeout(() => setLockSuccess(null), 2000);
  } else {
    // Show "Field locked by X" snackbar
    enqueueSnackbar(`Field locked by ${getLockOwner(fieldName)}`, {
      variant: 'warning',
      autoHideDuration: 3000,
    });
  }
};
```

**Stale Mode UX:**
- [ ] Pulsing animation on stale badge
- [ ] Auto-refresh button when MUTATED event received
- [ ] Countdown timer: "Refreshing in 5s..."

---

### 4. Edge Cases (0.3h)

**Handle:**
- [ ] User closes browser tab → auto-cleanup (already handled by WebSocket)
- [ ] Network interruption → auto-reconnect (already implemented)
- [ ] Multiple tabs open → each tab gets own session
- [ ] User logs out → cleanup all locks and presence
- [ ] Entity deleted while viewing → show error + redirect

**Cleanup on Unmount:**
```tsx
useEffect(() => {
  return () => {
    // Release all locks
    if (focusedField) {
      releaseLock(focusedField);
    }
    // Unsubscribe handled by usePresence hook
  };
}, [focusedField]);
```

---

### 5. Accessibility (0.2h)

**Add ARIA labels:**
- [ ] `aria-label` on presence indicator
- [ ] `aria-live="polite"` for stale mode alerts
- [ ] `role="status"` for connection status
- [ ] Keyboard navigation for lock indicators

**Implementation:**
```tsx
<Box
  role="status"
  aria-label="Real-time presence tracking"
  aria-live="polite"
  data-testid="presence-indicator"
>
  {/* Connection status */}
  <Box aria-label={connected ? 'Connected' : 'Disconnected'}>
    {/* ... */}
  </Box>
  
  {/* Stale mode */}
  {isStale && (
    <Alert 
      severity="warning"
      role="alert"
      aria-live="assertive"
    >
      {/* ... */}
    </Alert>
  )}
</Box>
```

---

## 📊 Acceptance Criteria

- [ ] All components have loading states
- [ ] Error boundaries catch and display errors gracefully
- [ ] Users get visual feedback for all actions (lock, connect, disconnect)
- [ ] Edge cases handled (network loss, multiple tabs, logout)
- [ ] ARIA labels added for screen readers
- [ ] No console errors or warnings
- [ ] TypeScript compiles with 0 errors
- [ ] Components render correctly in all states (loading, error, success)

---

## 🧪 Testing Checklist

Manual testing scenarios:
- [ ] Open page → see loading skeleton → see connected state
- [ ] Disconnect WiFi → see reconnecting badge → reconnect → see connected
- [ ] Focus field → see lock success feedback
- [ ] Try to focus locked field → see warning toast
- [ ] Trigger MUTATED event → see stale badge → see auto-refresh countdown
- [ ] Open in 2 tabs → verify both work independently
- [ ] Logout → verify cleanup (check Redis keys removed)
- [ ] Use screen reader → verify all labels are announced

---

## 📁 Files to Create/Modify

**New Files:**
1. `frontend/src/components/presence/PresenceErrorBoundary.tsx` (~100 lines)
2. `frontend/src/components/presence/PresenceLoadingSkeleton.tsx` (~50 lines)

**Modified Files:**
1. `frontend/src/components/presence/PresenceIndicator.tsx` - Add loading/error states
2. `frontend/src/components/presence/FieldLockIndicator.tsx` - Add animation, ARIA
3. `frontend/src/lib/presence/usePresence.ts` - Add loading/reconnecting state
4. `frontend/src/pages/Admin/UserEditPage.tsx` - Wrap in ErrorBoundary, add feedback

---

**Next Phase:** Phase 8 - Integration with more pages (TenantEditPage, RoleEditPage)
