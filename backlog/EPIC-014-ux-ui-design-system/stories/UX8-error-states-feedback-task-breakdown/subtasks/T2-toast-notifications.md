# T2: Toast Notification System

**Parent Story:** S8 - Error States & Feedback  
**LOC:** ~250 | **Effort:** ~3h

## Objective
Implement toast notification system with auto-dismiss and queue management.

## Implementation

```tsx
// frontend/src/components/feedback/ToastProvider.tsx
import { Snackbar, Alert, AlertColor } from '@mui/material';
import { createContext, useContext, useState } from 'react';

interface Toast {
  id: string;
  message: string;
  severity: AlertColor;
  duration?: number;
}

interface ToastContextType {
  showToast: (message: string, severity?: AlertColor, duration?: number) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  warning: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastContextType>(null!);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([]);
  
  const showToast = (message: string, severity: AlertColor = 'info', duration = 5000) => {
    const id = Date.now().toString();
    setToasts(prev => [...prev, { id, message, severity, duration }]);
  };
  
  const handleClose = (id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };
  
  const contextValue: ToastContextType = {
    showToast,
    success: (msg) => showToast(msg, 'success'),
    error: (msg) => showToast(msg, 'error', 7000),
    warning: (msg) => showToast(msg, 'warning'),
    info: (msg) => showToast(msg, 'info'),
  };
  
  return (
    <ToastContext.Provider value={contextValue}>
      {children}
      {toasts.map(toast => (
        <Snackbar
          key={toast.id}
          open
          autoHideDuration={toast.duration}
          onClose={() => handleClose(toast.id)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        >
          <Alert 
            severity={toast.severity} 
            onClose={() => handleClose(toast.id)}
            variant="filled"
          >
            {toast.message}
          </Alert>
        </Snackbar>
      ))}
    </ToastContext.Provider>
  );
};

export const useToast = () => useContext(ToastContext);

// Usage example
const MyComponent = () => {
  const toast = useToast();
  
  const handleSave = async () => {
    try {
      await api.save();
      toast.success('Saved successfully!');
    } catch (error) {
      toast.error('Failed to save');
    }
  };
};
```

## Acceptance Criteria
- [ ] Success/error/warning/info toasts
- [ ] Auto-dismiss after 5s (configurable)
- [ ] Manually closeable (X button)
- [ ] Queue multiple toasts
- [ ] Position bottom-right
- [ ] Stack vertically
- [ ] Accessible (role="alert")

## Files
- `frontend/src/components/feedback/ToastProvider.tsx`
- `frontend/src/hooks/useToast.ts`
