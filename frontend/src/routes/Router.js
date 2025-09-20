import React, { lazy } from 'react';
import { createBrowserRouter, Navigate } from 'react-router';

/* ***Layouts**** */
const FullLayout = lazy(() => import('../layouts/full/FullLayout'));

/* ****Production Pages***** */
const MePage = lazy(() => import('../views/profile/MePage'));
const MyProfilePage = lazy(() => import('../views/profile/MyProfilePage'));
const UserManagementPage = lazy(() => import('../views/admin/UserManagementPage'));

/* ****Demo Pages for Reference***** */
const Dashboard = lazy(() => import('../views/dashboard/Dashboard'));
const SamplePage = lazy(() => import('../views/sample-page/SamplePage'));

/**
 * 🚀 Production Router with User Management
 * 
 * Hlavní aplikace obsahuje:
 * - /me - rychlý přehled profilu (původní MePage)
 * - /profile - kompletní správa profilu
 * - /admin/users - administrace uživatelů (pouze pro admin role)
 * - /dashboard - demo dashboard (pro reference)
 * 
 * Role-based security je implementována na backend API úrovni
 * Frontend kontrola rolí se provádí v komponentách
 */
const Router = [
  {
    path: '/',
    element: <FullLayout />,
    children: [
      // 🏠 Homepage redirect to user profile
      { path: '/', element: <Navigate to="/me" /> },
      
      // 👤 User profile pages
      { path: '/me', element: <MePage /> },
      { path: '/profile', element: <MyProfilePage /> },
      
      // 🔧 Admin pages
      { path: '/admin/users', element: <UserManagementPage /> },
      
      // 📊 Demo/Reference pages
      { path: '/dashboard', element: <Dashboard /> },
      { path: '/sample-page', element: <SamplePage /> },
      
      // 🔄 Alternative routes and redirects
      { path: '/user-profile', element: <Navigate to="/profile" replace /> },
      
      // 🔄 Fallback for unknown routes
      { path: '*', element: <Navigate to="/me" /> },
    ],
  },
];

const router = createBrowserRouter(Router);

export default router;
