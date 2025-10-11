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
import Groups from './components/Groups.jsx';
import Layout from './components/Layout.jsx';
import DataTablePage from './pages/Examples/DataTablePage.tsx';
import KanbanPage from './pages/Examples/KanbanPage.tsx';

// 📊 Reporting with Grafana Scenes
import Reports from './pages/Reports.jsx';

// 📊 Reporting Explorer (Phase 3)
import { ReportingPage } from './components/Reporting';

// 🆕 Admin sekce
import {
  MonitoringPage,
  AdminUsersPage,
  AdminRolesPage,
  AdminTenantsPage,
  AdminSecurityPage,
  AdminAuditPage,
  KeycloakSyncPage,
  SyncHistoryPage,
  StreamingDashboardPage,
} from './pages/Admin';

// 🆕 S2: Presence System - User Edit Page
import UserEditPage from './pages/Admin/UserEditPage.tsx';

// 🆕 Tenant Admin sekce
import {
  TenantDashboard,
  TenantUsersPage,
  TenantRolesPage,
  TenantKeycloakSyncPage,
} from './pages/TenantAdmin';

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
        gap: '32px',
        background: 'linear-gradient(135deg, #f5f5f7 0%, #e8e8ea 100%)',
        padding: '20px'
      }}>
        {/* Loading Card */}
        <div style={{
          background: 'rgba(255, 255, 255, 0.9)',
          borderRadius: '24px',
          padding: '48px 64px',
          backdropFilter: 'blur(20px)',
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.08)',
          border: '1px solid rgba(255, 255, 255, 0.5)',
          textAlign: 'center',
          maxWidth: '500px',
          width: '100%'
        }}>
          {/* Animated Logo */}
          <div style={{ marginBottom: '32px' }}>
            <svg width="80" height="80" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" 
              style={{ 
                display: 'inline-block',
                animation: 'pulse 2s ease-in-out infinite'
              }}>
              <defs>
                <linearGradient id="loadingGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style={{stopColor: '#1976d2', stopOpacity: 1}} />
                  <stop offset="100%" style={{stopColor: '#42a5f5', stopOpacity: 1}} />
                </linearGradient>
              </defs>
              <path d="M20 2 L35 15 L30 15 L20 8 L10 15 L5 15 Z" fill="url(#loadingGradient)" />
              <path d="M8 18 L12 18 L20 28 L28 18 L32 18 L20 33 Z" fill="url(#loadingGradient)" />
              <rect x="18" y="16" width="4" height="18" fill="url(#loadingGradient)" opacity="0.8" />
            </svg>
          </div>

          {/* Loading Text */}
          <h2 style={{ 
            fontSize: '28px', 
            margin: '0 0 12px 0',
            background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
            fontWeight: 600,
          }}>
            Připojuji se...
          </h2>
          
          <p style={{ 
            fontSize: '16px', 
            margin: '0 0 32px 0', 
            color: '#666',
            lineHeight: '1.6'
          }}>
            Navazuji zabezpečené spojení s autentifikačním serverem
          </p>

          {/* Animated Progress Dots */}
          <div style={{ 
            display: 'flex', 
            justifyContent: 'center', 
            gap: '12px',
            marginBottom: '24px'
          }}>
            <div style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #1976d2, #42a5f5)',
              animation: 'bounce 1.4s ease-in-out 0s infinite'
            }} />
            <div style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #1976d2, #42a5f5)',
              animation: 'bounce 1.4s ease-in-out 0.2s infinite'
            }} />
            <div style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #1976d2, #42a5f5)',
              animation: 'bounce 1.4s ease-in-out 0.4s infinite'
            }} />
          </div>

          {/* Status Steps */}
          <div style={{
            background: 'rgba(25, 118, 210, 0.05)',
            borderRadius: '12px',
            padding: '20px',
            textAlign: 'left'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
              <div style={{ 
                width: '24px', 
                height: '24px', 
                borderRadius: '50%', 
                background: 'linear-gradient(135deg, #1976d2, #42a5f5)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: '12px',
                fontWeight: 'bold',
                animation: 'spin 1s linear infinite'
              }}>⟳</div>
              <span style={{ color: '#1976d2', fontWeight: 500 }}>Ověřuji Keycloak server</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
              <div style={{ 
                width: '24px', 
                height: '24px', 
                borderRadius: '50%', 
                background: '#e0e0e0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#999',
                fontSize: '16px'
              }}>○</div>
              <span style={{ color: '#999' }}>Načítám konfiguraci</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ 
                width: '24px', 
                height: '24px', 
                borderRadius: '50%', 
                background: '#e0e0e0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#999',
                fontSize: '16px'
              }}>○</div>
              <span style={{ color: '#999' }}>Připravuji přihlášení</span>
            </div>
          </div>
        </div>

        {/* CSS Animations */}
        <style>{`
          @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.8; transform: scale(0.95); }
          }
          
          @keyframes bounce {
            0%, 80%, 100% { transform: translateY(0); }
            40% { transform: translateY(-12px); }
          }
          
          @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }
        `}</style>
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
        {/* Logo Axiom */}
        <div style={{ marginBottom: '24px' }}>
          <svg width="64" height="64" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ display: 'inline-block' }}>
            <defs>
              <linearGradient id="axiomMainGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style={{stopColor: '#1976d2', stopOpacity: 1}} />
                <stop offset="100%" style={{stopColor: '#42a5f5', stopOpacity: 1}} />
              </linearGradient>
            </defs>
            <path d="M20 2 L35 15 L30 15 L20 8 L10 15 L5 15 Z" fill="url(#axiomMainGradient)" />
            <path d="M8 18 L12 18 L20 28 L28 18 L32 18 L20 33 Z" fill="url(#axiomMainGradient)" />
            <rect x="18" y="16" width="4" height="18" fill="url(#axiomMainGradient)" opacity="0.8" />
          </svg>
        </div>
        
        <h1 style={{ 
          fontSize: '42px', 
          margin: '20px 0 0 0', 
          background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          backgroundClip: 'text',
          fontWeight: 700,
        }}>
          Axiom
        </h1>
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
          {/* Logo Axiom */}
          <div style={{ marginBottom: '20px' }}>
            <svg width="48" height="48" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ display: 'inline-block' }}>
              <defs>
                <linearGradient id="axiomErrorGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style={{stopColor: '#1976d2', stopOpacity: 1}} />
                  <stop offset="100%" style={{stopColor: '#42a5f5', stopOpacity: 1}} />
                </linearGradient>
              </defs>
              <path d="M20 2 L35 15 L30 15 L20 8 L10 15 L5 15 Z" fill="url(#axiomErrorGradient)" />
              <path d="M8 18 L12 18 L20 28 L28 18 L32 18 L20 33 Z" fill="url(#axiomErrorGradient)" />
              <rect x="18" y="16" width="4" height="18" fill="url(#axiomErrorGradient)" opacity="0.8" />
            </svg>
          </div>
          
          <strong style={{ 
            fontSize: '18px', 
            display: 'block', 
            marginBottom: '12px',
            background: 'linear-gradient(135deg, #d32f2f 0%, #f44336 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}>
            ⚠️ Připojení se nezdařilo
          </strong>
          
          <div style={{ 
            fontSize: '14px', 
            lineHeight: '1.6',
            color: '#666',
            marginBottom: '16px',
          }}>
            Nepodařilo se připojit k autentizačnímu serveru.<br/>
            Zkontrolujte prosím připojení k internetu.
          </div>
          
          <div style={{
            fontSize: '12px',
            padding: '8px 12px',
            background: 'rgba(211, 47, 47, 0.05)',
            borderRadius: '6px',
            border: '1px solid rgba(211, 47, 47, 0.1)',
            color: '#999',
          }}>
            💡 Zkuste obnovit stránku nebo se přihlaste později
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
    // Show enhanced loading screen while initializing
    return <LoginPage />;
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  // ✅ Show loading while user data is being fetched (authenticated but no user yet)
  if (!user) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        background: 'linear-gradient(135deg, #f5f5f7 0%, #e8e8ea 100%)',
      }}>
        <div style={{
          background: 'rgba(255, 255, 255, 0.9)',
          borderRadius: '20px',
          padding: '40px',
          textAlign: 'center',
          backdropFilter: 'blur(20px)',
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.08)',
        }}>
          <div style={{ marginBottom: '20px' }}>
            <svg width="60" height="60" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" 
              style={{ animation: 'pulse 2s ease-in-out infinite' }}>
              <defs>
                <linearGradient id="userLoadingGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style={{stopColor: '#1976d2', stopOpacity: 1}} />
                  <stop offset="100%" style={{stopColor: '#42a5f5', stopOpacity: 1}} />
                </linearGradient>
              </defs>
              <path d="M20 2 L35 15 L30 15 L20 8 L10 15 L5 15 Z" fill="url(#userLoadingGradient)" />
              <path d="M8 18 L12 18 L20 28 L28 18 L32 18 L20 33 Z" fill="url(#userLoadingGradient)" />
              <rect x="18" y="16" width="4" height="18" fill="url(#userLoadingGradient)" opacity="0.8" />
            </svg>
          </div>
          <h3 style={{ 
            margin: '0 0 8px 0', 
            color: '#1976d2',
            fontSize: '20px',
            fontWeight: 600
          }}>
            Načítám uživatelský profil...
          </h3>
          <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>
            Připravuji vaše pracovní prostředí
          </p>
        </div>
      </div>
    );
  }

  return (
    <Layout user={user} onLogout={logout}>
      <Routes>
        {/* Hlavní dashboard */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard user={user} />} />
        
        {/* 📊 Reports - Grafana Scenes with BFF */}
        <Route path="/reports" element={<Reports />} />
        
        {/* 📊 Reporting Explorer - Advanced data grid & charts (Phase 3) */}
        <Route path="/reporting" element={<ReportingPage />} />
        
        {/* User Directory - veřejně přístupný */}
        <Route path="/user-directory" element={<UserDirectory user={user} />} />
        <Route path="/profile" element={<Profile user={user} />} />
        
        {/* 🆕 Core Admin sekce - hierarchická struktura */}
        <Route path="/core-admin">
          <Route index element={<Navigate to="/core-admin/monitoring" replace />} />
          <Route path="monitoring" element={<MonitoringPage />} />
          <Route path="users" element={<AdminUsersPage user={user} />} />
          <Route path="users/:userId/edit" element={<UserEditPage />} />
          <Route path="roles" element={<AdminRolesPage />} />
          <Route path="groups" element={<Groups user={user} />} />
          <Route path="tenants" element={<AdminTenantsPage user={user} />} />
          <Route path="security" element={<AdminSecurityPage />} />
          <Route path="audit" element={<AdminAuditPage />} />
          <Route path="keycloak-sync" element={<KeycloakSyncPage user={user} />} />
          <Route path="sync-history" element={<SyncHistoryPage />} />
          <Route path="streaming" element={<StreamingDashboardPage />} />
        </Route>
        
        {/* 🆕 Tenant Admin sekce - pro tenant administrátory */}
        <Route path="/tenant-admin">
          <Route index element={<TenantDashboard user={user} />} />
          <Route path="users" element={<TenantUsersPage user={user} />} />
          <Route path="roles" element={<TenantRolesPage user={user} />} />
          <Route path="groups" element={<Groups user={user} />} />
          <Route path="keycloak-sync" element={<TenantKeycloakSyncPage user={user} />} />
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
