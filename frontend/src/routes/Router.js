import React, { lazy } from 'react';
import { createBrowserRouter, Navigate } from 'react-router';

/* ***Layouts**** */
const FullLayout = lazy(() => import('../layouts/full/FullLayout'));

/* ****Production Pages***** */
const ProfilePage = lazy(() => import('../views/profile/ProfilePage'));
const UserManagementPage = lazy(() => import('../views/admin/UserManagementPage'));
const TenantManagementPage = lazy(() => import('../views/admin/TenantManagementPage'));

/* ****Directory Pages***** */
const UserDirectoryPage = lazy(() => import('../views/directory/UserDirectoryPage'));
const UserDetailPage = lazy(() => import('../views/directory/UserDetailPage'));

/**
 * 🚀 Production Router - Extended with Tenant Management
 * 
 * Hlavní aplikace obsahuje produkční stránky s hierarchií rolí:
 * - /profile - sjednocená správa profilu s organizační strukturou a zástupstvím
 * - /admin/users - administrace uživatelů (CORE_ROLE_USER_MANAGER+)
 * - /admin/tenants - správa tenantů (CORE_ROLE_TENANT_MANAGER+)
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
const Router = [
  {
    path: '/',
    element: <FullLayout />,
    children: [
      // 🏠 Homepage redirect to profile
      { path: '/', element: <Navigate to="/profile" /> },
      
      // 👤 User profile pages
      { path: '/me', element: <Navigate to="/profile" replace /> },
      { path: '/profile', element: <ProfilePage /> },
      
      // 📁 Directory pages (accessible to all authenticated users)
      { path: '/directory', element: <UserDirectoryPage /> },
      { path: '/directory/:userId', element: <UserDetailPage /> },
      
      // 🔧 Admin pages with role-based access
      { path: '/admin/users', element: <UserManagementPage /> }, // CORE_ROLE_USER_MANAGER+
      { path: '/admin/tenants', element: <TenantManagementPage /> }, // CORE_ROLE_TENANT_MANAGER+
      
      // 🔄 Redirects for old routes
      { path: '/user-profile', element: <Navigate to="/profile" replace /> },
      
      // 🔄 Fallback for unknown routes
      { path: '*', element: <Navigate to="/profile" /> },
    ],
  },
];

const router = createBrowserRouter(Router);

export default router;
