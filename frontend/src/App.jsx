import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { AuthProvider, useAuth } from './components/AuthProvider.jsx';
import { corePlatformTheme } from './shared/theme/theme.ts';

// Import komponent
import Dashboard from './components/Dashboard.jsx';
import Users from './components/Users.jsx';
import UserDirectory from './components/UserDirectory.jsx';
import Profile from './components/Profile.jsx';
import Tenants from './components/Tenants.jsx';
import TenantManagement from './components/TenantManagement.jsx';
import Layout from './components/Layout.jsx';
import DataTablePage from './pages/Examples/DataTablePage.tsx';
import KanbanPage from './pages/Examples/KanbanPage.tsx';

// Login komponenta
const LoginPage = () => {
  const { login, loading, error, keycloakInitialized } = useAuth();

  const handleLogin = () => {
    login();
  };

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        flexDirection: 'column',
        gap: '20px'
      }}>
        <div>🔧 Inicializuji autentifikaci...</div>
        <div style={{ fontSize: '14px', color: '#666' }}>
          Připojuji se k Keycloak serveru
        </div>
      </div>
    );
  }

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh',
      flexDirection: 'column',
      gap: '30px',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      color: 'white'
    }}>
      <div style={{ textAlign: 'center' }}>
        <h1 style={{ fontSize: '48px', margin: '0 0 10px 0' }}>🚀 Core Platform</h1>
        <p style={{ fontSize: '18px', margin: '0', opacity: 0.9 }}>
          Enterprise aplikační platforma
        </p>
      </div>
      
      {error && (
        <div style={{
          background: 'rgba(244, 67, 54, 0.1)',
          border: '1px solid rgba(244, 67, 54, 0.3)',
          borderRadius: '8px',
          padding: '15px',
          color: '#ffcdd2',
          textAlign: 'center',
          maxWidth: '400px'
        }}>
          <strong>Chyba přihlášení:</strong><br />
          {error}
        </div>
      )}
      
      <div style={{ textAlign: 'center' }}>
        <button
          onClick={handleLogin}
          disabled={!keycloakInitialized}
          style={{
            background: keycloakInitialized 
              ? 'linear-gradient(45deg, #4caf50, #45a049)' 
              : '#999',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            padding: '15px 30px',
            fontSize: '16px',
            fontWeight: '600',
            cursor: keycloakInitialized ? 'pointer' : 'not-allowed',
            boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
            transition: 'all 0.3s ease',
            transform: keycloakInitialized ? 'scale(1)' : 'scale(0.95)',
          }}
        >
          {keycloakInitialized ? '🔐 Přihlásit se přes Keycloak' : '⏳ Inicializuji Keycloak...'}
        </button>
      </div>
      
      <div style={{ 
        fontSize: '12px', 
        opacity: 0.7, 
        textAlign: 'center',
        maxWidth: '300px'
      }}>
        Budete přesměrováni na Keycloak server pro bezpečné přihlášení
      </div>
    </div>
  );
};

// Protected Route wrapper
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return <LoginPage />;
  }
  
  return isAuthenticated ? children : <LoginPage />;
};

// Main App Content
const AppContent = () => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <LoginPage />;
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <Layout>
      <Routes>
        {/* Hlavní dashboard */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        
        {/* User management */}
        <Route path="/users" element={<Users />} />
        <Route path="/user-directory" element={<UserDirectory />} />
        <Route path="/profile" element={<Profile />} />
        
        {/* Tenant management */}
        <Route path="/tenants" element={<Tenants />} />
        <Route path="/tenant-management" element={<TenantManagement />} />
        
        {/* Example pages */}
        <Route path="/examples/data-table" element={<DataTablePage />} />
        <Route path="/examples/kanban" element={<KanbanPage />} />
        
        {/* Fallback */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Layout>
  );
};

// Root App Component
function App() {
  return (
    <ThemeProvider theme={corePlatformTheme}>
      <CssBaseline />
      <Router>
        <AuthProvider>
          <AppContent />
        </AuthProvider>
      </Router>
    </ThemeProvider>
  );
}

export default App;
