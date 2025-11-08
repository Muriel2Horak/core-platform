# T1: Error Message Components

**Parent Story:** S8 - Error States & Feedback  
**LOC:** ~200 | **Effort:** ~3h

## Objective
Create consistent error message components with clear, actionable feedback.

## Implementation

```tsx
// frontend/src/components/errors/ErrorMessage.tsx
import { Alert, AlertTitle, Button } from '@mui/material';

export const ErrorMessage: React.FC<{
  title?: string;
  message: string;
  onRetry?: () => void;
  severity?: 'error' | 'warning' | 'info';
}> = ({ title, message, onRetry, severity = 'error' }) => (
  <Alert
    severity={severity}
    action={
      onRetry && (
        <Button color="inherit" size="small" onClick={onRetry}>
          Retry
        </Button>
      )
    }
  >
    {title && <AlertTitle>{title}</AlertTitle>}
    {message}
  </Alert>
);

// frontend/src/components/errors/ErrorBoundary.tsx
export class ErrorBoundary extends React.Component<
  { children: React.ReactNode; fallback?: React.ReactNode },
  { hasError: boolean; error: Error | null }
> {
  state = { hasError: false, error: null };
  
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }
  
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
    // Send to error tracking service (Sentry, etc.)
  }
  
  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <ErrorMessage 
          title="Something went wrong"
          message={this.state.error?.message || 'Unknown error'}
          onRetry={() => this.setState({ hasError: false, error: null })}
        />
      );
    }
    return this.props.children;
  }
}

// frontend/src/pages/NotFound.tsx
export const NotFound: React.FC = () => (
  <Box sx={{ textAlign: 'center', py: 8 }}>
    <Typography variant="h1" gutterBottom>404</Typography>
    <Typography variant="h5" color="text.secondary" gutterBottom>
      Page Not Found
    </Typography>
    <Button variant="contained" component={Link} to="/">
      Go Home
    </Button>
  </Box>
);

// Network error handler
export const NetworkError: React.FC<{ onRetry: () => void }> = ({ onRetry }) => (
  <ErrorMessage
    title="Network Error"
    message="Unable to connect to server. Please check your internet connection."
    onRetry={onRetry}
  />
);
```

## Acceptance Criteria
- [ ] Clear, user-friendly error messages
- [ ] Retry button when applicable
- [ ] Error boundary catches crashes
- [ ] 404 page styled
- [ ] Network error handling
- [ ] Validation errors inline
- [ ] Error logging to console/service

## Files
- `frontend/src/components/errors/ErrorMessage.tsx`
- `frontend/src/components/errors/ErrorBoundary.tsx`
- `frontend/src/components/errors/NetworkError.tsx`
- `frontend/src/pages/NotFound.tsx`
