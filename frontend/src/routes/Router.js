import React, { lazy } from 'react';
import { createBrowserRouter, Navigate } from 'react-router';

/* ***Layouts**** */
const FullLayout = lazy(() => import('../layouts/full/FullLayout'));

/* ****Production Pages***** */
const ProfilePage = lazy(() => import('../views/profile/ProfilePage'));
const UserManagementPage = lazy(() => import('../views/admin/UserManagementPage'));

/**
 * 🚀 Production Router - Clean Version
 * 
 * Hlavní aplikace obsahuje pouze produkční stránky:
 * - /profile - sjednocená správa profilu s organizační strukturou a zástupstvím
 * - /admin/users - administrace uživatelů (pouze pro admin role)
 * 
 * Role-based security je implementována na backend API úrovni
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
      
      // 🔧 Admin pages
      { path: '/admin/users', element: <UserManagementPage /> },
      
      // 🔄 Redirects for old routes
      { path: '/user-profile', element: <Navigate to="/profile" replace /> },
      
      // 🔄 Fallback for unknown routes
      { path: '*', element: <Navigate to="/profile" /> },
    ],
  },
];

const router = createBrowserRouter(Router);

export default router;
