import { useState, useEffect } from 'react';
import authService from '../services/auth';
import logger from '../services/logger';

/**
 * 👑 USER ROLES HOOK
 * 
 * Custom hook pro práci s uživatelskými rolemi a permissions
 */
const useUserRoles = () => {
  const [userRoles, setUserRoles] = useState([]);
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadUserRoles();
  }, []);

  const loadUserRoles = async () => {
    try {
      setLoading(true);
      setError(null);

      const userInfo = await authService.getUserInfo();
      const roles = userInfo?.roles || [];
      
      setUserRoles(roles);
      setUserInfo(userInfo);
      
      console.log('🔐 User roles loaded:', roles);
      logger.userAction('ROLES_LOADED_FROM_TOKEN', { roles });
      
    } catch (err) {
      console.error('Failed to load user roles:', err);
      setError(err.message);
      setUserRoles([]);
      logger.error('ROLES_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 🔍 Check if user has any of the specified roles
   */
  const hasAnyRole = (requiredRoles) => {
    if (!requiredRoles || requiredRoles.length === 0) {
      return true; // No roles required
    }

    if (loading || userRoles.length === 0) {
      return false;
    }

    return requiredRoles.some(role => userRoles.includes(role));
  };

  /**
   * 🔍 Check if user has ALL specified roles
   */
  const hasAllRoles = (requiredRoles) => {
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    if (loading || userRoles.length === 0) {
      return false;
    }

    return requiredRoles.every(role => userRoles.includes(role));
  };

  /**
   * 🔍 Check if user has specific role
   */
  const hasRole = (role) => {
    return hasAnyRole([role]);
  };

  /**
   * 👑 Get user's highest role in hierarchy
   */
  const getHighestRole = () => {
    const roleHierarchy = [
      'CORE_ROLE_ADMIN',
      'CORE_ROLE_SYSTEM_ADMIN', 
      'CORE_ROLE_TENANT_MANAGER',
      'CORE_ROLE_TENANT_ADMIN',
      'CORE_ROLE_USER_MANAGER',
      'CORE_ROLE_USER'
    ];

    for (const role of roleHierarchy) {
      if (userRoles.includes(role)) {
        return role;
      }
    }
    return null;
  };

  /**
   * 🔒 Check if user is admin (any admin role)
   */
  const isAdmin = () => {
    const adminRoles = [
      'CORE_ROLE_ADMIN',
      'CORE_ROLE_SYSTEM_ADMIN',
      'CORE_ROLE_TENANT_MANAGER',
      'CORE_ROLE_TENANT_ADMIN'
    ];
    return hasAnyRole(adminRoles);
  };

  /**
   * 🏢 Check if user can manage tenants
   */
  const canManageTenants = () => {
    return hasAnyRole([
      'CORE_ROLE_ADMIN',
      'CORE_ROLE_SYSTEM_ADMIN',
      'CORE_ROLE_TENANT_MANAGER'
    ]);
  };

  /**
   * 👥 Check if user can manage users
   */
  const canManageUsers = () => {
    return hasAnyRole([
      'CORE_ROLE_ADMIN',
      'CORE_ROLE_SYSTEM_ADMIN',
      'CORE_ROLE_TENANT_MANAGER',
      'CORE_ROLE_TENANT_ADMIN',
      'CORE_ROLE_USER_MANAGER'
    ]);
  };

  /**
   * 🎯 Get role display name
   */
  const getRoleDisplayName = (role) => {
    const roleNames = {
      'CORE_ROLE_ADMIN': 'Super Administrator',
      'CORE_ROLE_SYSTEM_ADMIN': 'System Administrator',
      'CORE_ROLE_TENANT_MANAGER': 'Tenant Manager',
      'CORE_ROLE_TENANT_ADMIN': 'Tenant Administrator',
      'CORE_ROLE_USER_MANAGER': 'User Manager',
      'CORE_ROLE_USER': 'User'
    };
    return roleNames[role] || role;
  };

  /**
   * 🔄 Refresh roles (e.g., after role change)
   */
  const refreshRoles = () => {
    loadUserRoles();
  };

  return {
    userRoles,
    userInfo,
    loading,
    error,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    getHighestRole,
    isAdmin,
    canManageTenants,
    canManageUsers,
    getRoleDisplayName,
    refreshRoles
  };
};

export default useUserRoles;