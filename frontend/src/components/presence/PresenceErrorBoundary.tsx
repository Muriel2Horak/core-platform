import React, { Component, ReactNode } from 'react';
import { Alert, AlertTitle, Button, Box, Typography } from '@mui/material';
import { Refresh } from '@mui/icons-material';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: React.ErrorInfo | null;
}

/**
 * Error boundary for presence system components
 * 
 * Catches:
 * - WebSocket connection errors
 * - React component render errors
 * - Unexpected presence state errors
 * 
 * Shows user-friendly fallback UI with retry option
 */
export class PresenceErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    // Log to console (in production, send to monitoring service like Sentry)
    console.error('Presence Error Boundary caught error:', {
      error,
      errorInfo,
      componentStack: errorInfo.componentStack,
    });

    this.setState({
      errorInfo,
    });

    // TODO: Send to monitoring service
    // Sentry.captureException(error, { contexts: { react: errorInfo } });
  }

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      // Custom fallback UI
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // Default fallback UI
      return (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                size="small"
                onClick={this.handleReset}
                startIcon={<Refresh />}
              >
                Retry
              </Button>
              <Button
                size="small"
                variant="outlined"
                onClick={this.handleReload}
              >
                Reload Page
              </Button>
            </Box>
          }
        >
          <AlertTitle>Presence System Error</AlertTitle>
          <Typography variant="body2" sx={{ mb: 1 }}>
            Real-time presence tracking is temporarily unavailable.
          </Typography>
          {process.env.NODE_ENV === 'development' && this.state.error && (
            <Box
              component="pre"
              sx={{
                mt: 2,
                p: 1,
                bgcolor: 'grey.100',
                borderRadius: 1,
                fontSize: '0.75rem',
                overflow: 'auto',
                maxHeight: 200,
              }}
            >
              {this.state.error.toString()}
              {this.state.errorInfo?.componentStack}
            </Box>
          )}
        </Alert>
      );
    }

    return this.props.children;
  }
}
