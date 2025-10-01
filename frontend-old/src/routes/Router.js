import React, { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { withRouteGuard } from '../utils/ensureComponent.js';

/* ***Layouts**** */
const FullLayout = lazy(() => import('../layouts/full/FullLayout.js'));

/* ****Pages***** */
const Dashboard = lazy(() => import('../views/dashboard/Dashboard.js'));

/* ****Production Pages***** */
const ProfilePage = lazy(() => import('../views/profile/ProfilePage.js'));
const UserManagementPage = lazy(() => import('../views/admin/UserManagementPage.js'));
const TenantManagementPage = lazy(() => import('../views/admin/TenantManagementPage.js'));

/* ****Directory Pages***** */
const UserDirectoryPage = lazy(() => import('../views/directory/UserDirectoryPage.js'));
const UserDetailPage = lazy(() => import('../views/directory/UserDetailPage.js'));

// 🚧 DOČASNÁ DIAGNOSTIKA - Wrapper komponenty s guardy
const GuardedDashboard = withRouteGuard(Dashboard, 'Dashboard');
const GuardedProfilePage = withRouteGuard(ProfilePage, 'ProfilePage');
const GuardedUserManagementPage = withRouteGuard(UserManagementPage, 'UserManagementPage');
const GuardedTenantManagementPage = withRouteGuard(TenantManagementPage, 'TenantManagementPage');
const GuardedUserDirectoryPage = withRouteGuard(UserDirectoryPage, 'UserDirectoryPage');
const GuardedUserDetailPage = withRouteGuard(UserDetailPage, 'UserDetailPage');
const GuardedFullLayout = withRouteGuard(FullLayout, 'FullLayout');

// Diagnostický fallback komponent
const DiagnosticFallback = ({ routeName }) => (
  <div style={{ padding: '20px', textAlign: 'center' }}>
    <h3>🔄 Loading {routeName}...</h3>
    <p>Lazy loading component...</p>
  </div>
);

/**
 * 🚀 Production Router - Extended with Tenant Management
 * 🚧 DOČASNĚ s diagnostickými guardy pro React error #130
 * 
 * Hlavní aplikace obsahuje produkční stránky s hierarchií rolí:
 * - /profile - sjednocená správa profilu s organizační strukturou a zástupstvím
 * - /admin-core/users - administrace uživatelů (CORE_ROLE_USER_MANAGER+)
 * - /admin-core/tenants - správa tenantů (CORE_ROLE_TENANT_MANAGER+)
 * - /directory - User Directory přístupný všem uživatelům
 * - /directory/:userId - detail uživatele s možností editace
 * 
 * Role hierarchy:
 * - CORE_ROLE_USER: Základní uživatel
 * - CORE_ROLE_USER_MANAGER: Správa uživatelů v tenantu
 * - CORE_ROLE_TENANT_ADMIN: Admin tenantu (zahrnuje USER_MANAGER)
 * - CORE_ROLE_TENANT_MANAGER: Správa více tenantů
 * - CORE_ROLE_SYSTEM_ADMIN: Systémová administrace
 * - CORE_ROLE_ADMIN: Super admin (vytváření tenantů)
 */
const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <Suspense fallback={<DiagnosticFallback routeName="FullLayout" />}>
        <GuardedFullLayout />
      </Suspense>
    ),
    children: [
      // 🏠 Homepage redirect to dashboard (updated)
      { path: '/', element: <Navigate to="/dashboard" /> },
      
      // 📊 Dashboard
      { 
        path: '/dashboard', 
        element: (
          <Suspense fallback={<DiagnosticFallback routeName="Dashboard" />}>
            <GuardedDashboard />
          </Suspense>
        )
      },
      
      // 👤 User profile pages
      { path: '/me', element: <Navigate to="/profile" replace /> },
      { 
        path: '/profile', 
        element: (
          <Suspense fallback={<DiagnosticFallback routeName="ProfilePage" />}>
            <GuardedProfilePage />
          </Suspense>
        )
      },
      
      // 📁 Directory pages (accessible to all authenticated users)
      { 
        path: '/directory', 
        element: (
          <Suspense fallback={<DiagnosticFallback routeName="UserDirectoryPage" />}>
            <GuardedUserDirectoryPage />
          </Suspense>
        )
      },
      { 
        path: '/directory/:userId', 
        element: (
          <Suspense fallback={<DiagnosticFallback routeName="UserDetailPage" />}>
            <GuardedUserDetailPage />
          </Suspense>
        )
      },
      
      // 🔧 Admin pages with role-based access - RENAMED to avoid conflict with Keycloak /admin
      { 
        path: '/admin-core/users', 
        element: (
          <Suspense fallback={<DiagnosticFallback routeName="UserManagementPage" />}>
            <GuardedUserManagementPage />
          </Suspense>
        )
      }, // CORE_ROLE_USER_MANAGER+
      { 
        path: '/admin-core/tenants', 
        element: (
          <Suspense fallback={<DiagnosticFallback routeName="TenantManagementPage" />}>
            <GuardedTenantManagementPage />
          </Suspense>
        )
      }, // CORE_ROLE_TENANT_MANAGER+
      
      // 🔄 Redirects for old routes
      { path: '/user-profile', element: <Navigate to="/profile" replace /> },
      
      // 🔄 Fallback for unknown routes
      { path: '*', element: <Navigate to="/profile" /> },
    ],
  },
]);

export default router;
