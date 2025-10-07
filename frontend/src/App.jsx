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

// 🆕 Admin sekce
import {
  MonitoringPage,
  AdminUsersPage,
  AdminRolesPage,
  AdminTenantsPage,
  AdminSecurityPage,
  AdminAuditPage,
} from './pages/Admin';

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
      background: 'linear-gradient(135deg, #f5f5f7 0%, #e0e0e0 100%)',  // ✅ Světlý gradient místo fialové
      color: '#1a1a1a'  // ✅ Tmavý text místo bílé
    }}>
      <div style={{ 
        textAlign: 'center',
        padding: '40px',
        background: 'rgba(255, 255, 255, 0.8)',  // ✅ Glassmorphic karta
        borderRadius: '16px',
        backdropFilter: 'blur(20px)',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
        border: '1px solid rgba(255, 255, 255, 0.3)',
      }}>
        <h1 style={{ fontSize: '48px', margin: '0 0 10px 0', color: '#1976d2' }}>🚀 Core Platform</h1>
        <p style={{ fontSize: '18px', margin: '0', opacity: 0.7, color: '#6b6b6b' }}>
          Enterprise aplikační platforma
        </p>
      </div>
      
      {error && (
        <div style={{
          background: 'rgba(255, 255, 255, 0.9)',  // ✅ Bílý glassmorphic box
          border: '1px solid rgba(244, 67, 54, 0.3)',
          borderRadius: '12px',
          padding: '20px',
          color: '#d32f2f',  // ✅ Červená pro chybu
          textAlign: 'center',
          maxWidth: '400px',
          backdropFilter: 'blur(10px)',
          boxShadow: '0 4px 16px rgba(244, 67, 54, 0.1)',
        }}>
          <strong style={{ fontSize: '16px', display: 'block', marginBottom: '8px' }}>
            ⚠️ Chyba přihlášení
          </strong>
          <div style={{ fontSize: '14px', opacity: 0.8, color: '#6b6b6b' }}>
            Nepodařilo se připojit k autentizačnímu serveru. Zkuste to prosím znovu.
          </div>
          {/* ✅ Technická hláška skrytá - jen pro konzoli */}
          {console.error('Keycloak error:', error)}
        </div>
      )}
      
      <div style={{ textAlign: 'center' }}>
        <button
          onClick={handleLogin}
          disabled={!keycloakInitialized}
          style={{
            background: keycloakInitialized 
              ? 'linear-gradient(135deg, #1976d2, #1565c0)'  // ✅ Modrý gradient místo zelené
              : 'rgba(158, 158, 158, 0.5)',  // ✅ Glassmorphic disabled
            color: 'white',
            border: keycloakInitialized ? 'none' : '1px solid rgba(158, 158, 158, 0.3)',
            borderRadius: '12px',  // ✅ Větší radius pro glassmorphic styl
            padding: '16px 40px',
            fontSize: '16px',
            fontWeight: '600',
            cursor: keycloakInitialized ? 'pointer' : 'not-allowed',
            boxShadow: keycloakInitialized 
              ? '0 8px 24px rgba(25, 118, 210, 0.3)'  // ✅ Modrý stín
              : '0 4px 12px rgba(0, 0, 0, 0.1)',
            backdropFilter: 'blur(10px)',
            transition: 'all 0.3s ease',
            transform: keycloakInitialized ? 'scale(1)' : 'scale(0.98)',
          }}
        >
          {keycloakInitialized ? '🔐 Přihlásit se přes Keycloak' : '⏳ Inicializuji Keycloak...'}
        </button>
      </div>
      
      <div style={{ 
        fontSize: '12px', 
        color: '#6b6b6b',  // ✅ Tmavší text místo bílé
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
  const { isAuthenticated, loading, user, logout } = useAuth();

  if (loading) {
    return <LoginPage />;
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <Layout user={user} onLogout={logout}>
      <Routes>
        {/* Hlavní dashboard */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard user={user} />} />
        
        {/* User Directory - veřejně přístupný */}
        <Route path="/user-directory" element={<UserDirectory user={user} />} />
        <Route path="/profile" element={<Profile user={user} />} />
        
        {/* 🆕 Core Admin sekce - hierarchická struktura */}
        <Route path="/core-admin">
          <Route index element={<Navigate to="/core-admin/monitoring" replace />} />
          <Route path="monitoring" element={<MonitoringPage />} />
          <Route path="users" element={<AdminUsersPage user={user} />} />
          <Route path="roles" element={<AdminRolesPage />} />
          <Route path="tenants" element={<AdminTenantsPage user={user} />} />
          <Route path="security" element={<AdminSecurityPage />} />
          <Route path="audit" element={<AdminAuditPage />} />
        </Route>
        
        {/* Legacy routes - redirect na nové core-admin cesty */}
        <Route path="/admin" element={<Navigate to="/core-admin/monitoring" replace />} />
        <Route path="/admin/*" element={<Navigate to="/core-admin/monitoring" replace />} />
        <Route path="/users" element={<Navigate to="/core-admin/users" replace />} />
        <Route path="/tenants" element={<Navigate to="/core-admin/tenants" replace />} />
        <Route path="/tenant-management" element={<Navigate to="/admin/tenants" replace />} />
        
        {/* Example pages */}
        <Route path="/examples/data-table" element={<DataTablePage user={user} />} />
        <Route path="/examples/kanban" element={<KanbanPage user={user} />} />
        
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
