import React from 'react';
import { Box, Typography, Button, Alert } from '@mui/material';

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
  errorInfo?: React.ErrorInfo;
}

interface ErrorBoundaryProps {
  children: React.ReactNode;
  fallback?: React.ComponentType<{ error: Error; reset: () => void }>;
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('🚨 ErrorBoundary zachytil chybu:', error, errorInfo);
    
    // Specifické zpracování React error #130
    if (error.message.includes('Element type is invalid')) {
      console.error('❌ React error #130 zachycen - pravděpodobně undefined komponenta!');
    }
    
    this.setState({ error, errorInfo });
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  render() {
    if (this.state.hasError) {
      const { error } = this.state;
      
      // Pokud je poskytnut custom fallback, použij ho
      if (this.props.fallback) {
        const FallbackComponent = this.props.fallback;
        return <FallbackComponent error={error!} reset={this.handleReset} />;
      }

      // Výchozí error UI
      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '50vh',
            gap: 3,
            p: 3,
            textAlign: 'center'
          }}
        >
          <Alert severity="error" sx={{ maxWidth: 600 }}>
            <Typography variant="h6" gutterBottom>
              Ups, něco se pokazilo
            </Typography>
            <Typography variant="body2" sx={{ mb: 2 }}>
              {error?.message?.includes('Element type is invalid') 
                ? 'Komponenta se nepodařila načíst - zkontrolujte importy a exporty!'
                : error?.message || 'Neznámá chyba při renderování komponenty'
              }
            </Typography>
            {process.env.NODE_ENV === 'development' && (
              <Typography variant="caption" component="pre" sx={{ 
                textAlign: 'left', 
                backgroundColor: 'rgba(0,0,0,0.1)', 
                p: 1, 
                borderRadius: 1,
                fontSize: '0.75rem',
                overflow: 'auto'
              }}>
                {error?.stack}
              </Typography>
            )}
          </Alert>
          
          <Button variant="contained" onClick={this.handleReset}>
            Zkusit znovu
          </Button>
          
          <Button 
            variant="outlined" 
            onClick={() => window.location.reload()}
            size="small"
          >
            Obnovit stránku
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;