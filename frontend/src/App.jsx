import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline, Box, CircularProgress, Typography, Button } from '@mui/material';
import coreMaterialTheme from './styles/theme.js';
import { AuthProvider, useAuth } from './components/AuthProvider.jsx';
import Layout from './components/Layout.jsx';
import Dashboard from './components/Dashboard.jsx';
import Users from './components/Users.jsx';
import UserDirectory from './components/UserDirectory.jsx';
import Profile from './components/Profile.jsx';
import Tenants from './components/Tenants.jsx';
import TenantManagement from './components/TenantManagement.jsx';

// Loading component
const LoadingScreen = ({ message = 'Načítání aplikace...' }) => (
  <Box
    display="flex"
    flexDirection="column"
    justifyContent="center"
    alignItems="center"
    minHeight="100vh"
    sx={{ backgroundColor: 'background.default' }}
  >
    <CircularProgress size={60} sx={{ mb: 2 }} />
    <Typography variant="body1" color="text.secondary">
      {message}
    </Typography>
  </Box>
);

// Error component
const ErrorScreen = ({ message, onRetry }) => (
  <Box
    display="flex"
    flexDirection="column"
    justifyContent="center"
    alignItems="center"
    minHeight="100vh"
    sx={{ backgroundColor: 'background.default', p: 3 }}
  >
    <Typography variant="h4" color="error" gutterBottom>
      ❌ Chyba aplikace
    </Typography>
    <Typography variant="body1" color="text.secondary" sx={{ mb: 3, textAlign: 'center', maxWidth: 600 }}>
      {message}
    </Typography>
    {onRetry && (
      <Button 
        onClick={onRetry}
        variant="contained"
        color="primary"
        size="large"
      >
        Zkusit znovu
      </Button>
    )}
  </Box>
);

// Main App Content - uses Auth Context
const AppContent = () => {
  const { loading, error, user, isAuthenticated, login, logout } = useAuth();

  // Show loading screen
  if (loading) {
    return <LoadingScreen message="Načítání aplikace..." />;
  }

  // Show error screen
  if (error) {
    return <ErrorScreen message={error} onRetry={() => window.location.reload()} />;
  }

  // Redirect to Keycloak login if not authenticated - NO STEP 1!
  if (!isAuthenticated) {
    // Trigger login immediately - no intermediate screen
    React.useEffect(() => {
      login();
    }, [login]);
    
    return <LoadingScreen message="Přesměrování na přihlášení..." />;
  }

  // Main application
  return (
    <Router>
      <Layout user={user} onLogout={logout}>
        <Routes>
          <Route path="/" element={<Dashboard user={user} />} />
          <Route path="/dashboard" element={<Dashboard user={user} />} />
          <Route path="/profile" element={<Profile user={user} />} />
          <Route path="/users" element={<Users user={user} />} />
          <Route path="/user-directory" element={<UserDirectory user={user} />} />
          <Route path="/tenants" element={<Tenants user={user} />} />
          <Route path="/tenant-management" element={<TenantManagement user={user} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Layout>
    </Router>
  );
};

// Main App with AuthProvider
function App() {
  return (
    <ThemeProvider theme={coreMaterialTheme}>
      <CssBaseline />
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;