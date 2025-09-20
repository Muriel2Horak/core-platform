import { useState, useEffect } from 'react';
import authService from '../services/auth';
import userManagementService from '../services/userManagementService';
import logger from '../services/logger';

/**
 * Custom hook pro správu uživatelských rolí a oprávnění
 */
export const useUserRoles = () => {
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
      
      // Získat informace o aktuálním uživateli
      const user = await authService.getUserInfo();
      if (!user) {
        throw new Error('User not authenticated');
      }
      
      setUserInfo(user);
      
      // Pokud uživatel má role v JWT tokenu, použij je
      if (user.roles && Array.isArray(user.roles)) {
        setUserRoles(user.roles);
        logger.userAction('ROLES_LOADED_FROM_TOKEN', { roles: user.roles });
      } else {
        // Jinak načti role z API
        try {
          const profile = await userManagementService.getMyProfile();
          const roles = profile.roles || [];
          setUserRoles(roles);
          logger.userAction('ROLES_LOADED_FROM_API', { roles });
        } catch (apiError) {
          console.warn('Failed to load roles from API, fallback to empty roles', apiError);
          setUserRoles([]);
        }
      }
      
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
   * Kontrola, zda má uživatel konkrétní roli
   */
  const hasRole = (role) => {
    if (!role || !Array.isArray(userRoles)) return false;
    return userRoles.includes(role);
  };

  /**
   * Kontrola, zda má uživatel alespoň jednu z požadovaných rolí
   */
  const hasAnyRole = (roles) => {
    if (!roles || !Array.isArray(roles) || roles.length === 0) return true;
    if (!Array.isArray(userRoles)) return false;
    return roles.some(role => userRoles.includes(role));
  };

  /**
   * Kontrola, zda má uživatel všechny požadované role
   */
  const hasAllRoles = (roles) => {
    if (!roles || !Array.isArray(roles) || roles.length === 0) return true;
    if (!Array.isArray(userRoles)) return false;
    return roles.every(role => userRoles.includes(role));
  };

  /**
   * Kontrola admin oprávnění
   */
  const isAdmin = () => {
    return hasRole('admin');
  };

  /**
   * Kontrola user management oprávnění
   */
  const canManageUsers = () => {
    return hasAnyRole(['admin', 'user-manager']);
  };

  /**
   * Refresh rolí ze serveru
   */
  const refreshRoles = async () => {
    await loadUserRoles();
  };

  return {
    userRoles,
    userInfo,
    loading,
    error,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    isAdmin,
    canManageUsers,
    refreshRoles
  };
};

export default useUserRoles;