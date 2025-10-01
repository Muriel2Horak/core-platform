import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline, Box, CircularProgress, Typography } from '@mui/material';
import coreMaterialTheme from './styles/theme.js';
import apiService from './services/api.js';
import logger from './services/logger.js';
import keycloakService from './services/keycloakService.js';
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
    <Typography variant="h5" color="error" gutterBottom>
      Chyba při načítání aplikace
    </Typography>
    <Typography variant="body1" color="text.secondary" sx={{ mb: 3, textAlign: 'center' }}>
      {message}
    </Typography>
    {onRetry && (
      <button 
        onClick={onRetry}
        style={{
          padding: '10px 20px',
          backgroundColor: coreMaterialTheme.palette.primary.main,
          color: 'white',
          border: 'none',
          borderRadius: '7px',
          cursor: 'pointer'
        }}
      >
        Zkusit znovu
      </button>
    )}
  </Box>
);

function App() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(null);
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Initialize authentication using Keycloak service
  const initializeAuth = async () => {
    try {
      logger.info('🚀 Initializing Keycloak authentication...');
      setLoading(true);
      setError(null);
      
      // Initialize Keycloak
      const keycloak = await keycloakService.init();
      const authenticated = keycloak?.authenticated || false;
      
      logger.info('✅ Keycloak initialized successfully', {
        authenticated,
        realm: keycloak?.realm,
        clientId: keycloak?.clientId
      });
      
      setKeycloakInitialized(true);
      setIsAuthenticated(authenticated);

      if (authenticated) {
        // Get user info from Keycloak token
        const userInfo = keycloakService.getUserInfo();
        if (userInfo) {
          setUser(userInfo);
          
          // Set tenant context in logger
          logger.setTenantContext(userInfo.tenant, userInfo.username);
          
          // Set token in API service for backend calls
          const token = keycloakService.getToken();
          await apiService.createSession(token);
          
          logger.info('✅ User authenticated successfully', {
            username: userInfo.username,
            tenant: userInfo.tenant,
            roles: userInfo.roles?.length || 0,
            email: userInfo.email
          });
        } else {
          throw new Error('Failed to get user info from Keycloak token');
        }
      } else {
        logger.info('ℹ️ User not authenticated, ready for login redirect');
      }
      
    } catch (error) {
      logger.error('❌ Keycloak initialization failed', { 
        error: error.message,
        stack: error.stack 
      });
      
      const errorMessage = `Nepodařilo se inicializovat přihlášení: ${error.message}`;
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Handle logout
  const handleLogout = async () => {
    try {
      logger.info('🚪 Initiating logout...');
      
      // Clear API service session
      await apiService.logout();
      
      // Clear local state
      setUser(null);
      setIsAuthenticated(false);
      
      // Logout from Keycloak (this will redirect)
      keycloakService.logout();
      
    } catch (error) {
      logger.error('❌ Logout failed', { error: error.message });
      // Force logout redirect anyway
      keycloakService.logout();
    }
  };

  // Setup token refresh listener
  useEffect(() => {
    const handleTokenRefresh = (event) => {
      logger.info('🔄 Token refreshed, updating user info...');
      const newTenant = event.detail.tenant;
      
      // Update user info with new token data
      const updatedUserInfo = keycloakService.getUserInfo();
      if (updatedUserInfo) {
        setUser(updatedUserInfo);
        
        // Update tenant context if changed
        if (newTenant && newTenant !== user?.tenant) {
          logger.setTenantContext(newTenant, updatedUserInfo.username);
          logger.info(`✅ Tenant context updated to '${newTenant}'`);
        }
      }
    };

    window.addEventListener('keycloak-token-refreshed', handleTokenRefresh);
    return () => {
      window.removeEventListener('keycloak-token-refreshed', handleTokenRefresh);
    };
  }, [user?.tenant]);

  // Initialize on mount
  useEffect(() => {
    logger.info('🎬 App component mounted, starting Keycloak initialization...');
    initializeAuth();
  }, []);

  // Show loading screen during initialization
  if (loading || !keycloakInitialized) {
    return (
      <ThemeProvider theme={coreMaterialTheme}>
        <CssBaseline />
        <LoadingScreen message="Inicializuji přihlášení..." />
      </ThemeProvider>
    );
  }

  // Show error screen
  if (error) {
    return (
      <ThemeProvider theme={coreMaterialTheme}>
        <CssBaseline />
        <ErrorScreen message={error} onRetry={initializeAuth} />
      </ThemeProvider>
    );
  }

  // User not authenticated - redirect to Keycloak login
  if (!isAuthenticated) {
    logger.info('🔄 User not authenticated, redirecting to Keycloak login');
    keycloakService.login();
    return (
      <ThemeProvider theme={coreMaterialTheme}>
        <CssBaseline />
        <LoadingScreen message="Přesměrování na přihlášení..." />
      </ThemeProvider>
    );
  }

  // Guard for missing user info
  if (!user) {
    return (
      <ThemeProvider theme={coreMaterialTheme}>
        <CssBaseline />
        <ErrorScreen 
          message="Nepodařilo se načíst informace o uživateli" 
          onRetry={initializeAuth} 
        />
      </ThemeProvider>
    );
  }

  // Main application
  return (
    <ThemeProvider theme={coreMaterialTheme}>
      <CssBaseline />
      <Router>
        <Layout user={user} onLogout={handleLogout}>
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
    </ThemeProvider>
  );
}

export default App;