import { useState, useEffect, useCallback, useRef } from 'react';
import keycloakService from '../services/keycloakService';

/**
 * React hook pro správu aktuálních informací o uživateli
 * - Automaticky načítá fresh data z Keycloak
 * - Pravidelně obnovuje data
 * - Poskytuje loading state a error handling
 * - Optimalizuje API calls
 */
export const useUserInfo = (options = {}) => {
  const {
    refreshInterval = 5 * 60 * 1000, // 5 minut default
    autoRefresh = true,
    includeTokenInfo: _includeTokenInfo = true
  } = options;

  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  
  const refreshTimeoutRef = useRef(null);
  const isMountedRef = useRef(true);

  // Funkce pro načtení fresh user info
  const loadUserInfo = useCallback(async (showLoading = true) => {
    if (!keycloakService.isAuthenticated()) {
      setError(new Error('User not authenticated'));
      setLoading(false);
      return;
    }

    try {
      if (showLoading) {
        setLoading(true);
      }
      setError(null);

      console.log('🔄 useUserInfo: Načítám fresh user info...');
      const freshData = await keycloakService.getUserInfoFresh();
      
      if (isMountedRef.current) {
        console.log('🔄 useUserInfo: Fresh data načtena:', freshData);
        setUserInfo(freshData);
        setLastUpdated(new Date());
      }
    } catch (err) {
      console.error('🔄 useUserInfo: Chyba při načítání user info:', err);
      if (isMountedRef.current) {
        setError(err);
      }
    } finally {
      if (isMountedRef.current && showLoading) {
        setLoading(false);
      }
    }
  }, []);

  // Manuální refresh
  const refreshUserInfo = useCallback(() => {
    return loadUserInfo(false);
  }, [loadUserInfo]);

  // Nastavení automatického refreshe
  useEffect(() => {
    if (!autoRefresh || !refreshInterval) return;

    const scheduleNextRefresh = () => {
      if (refreshTimeoutRef.current) {
        clearTimeout(refreshTimeoutRef.current);
      }
      
      refreshTimeoutRef.current = setTimeout(() => {
        if (isMountedRef.current) {
          console.log('🔄 useUserInfo: Automatický refresh user info');
          refreshUserInfo();
          scheduleNextRefresh();
        }
      }, refreshInterval);
    };

    scheduleNextRefresh();

    return () => {
      if (refreshTimeoutRef.current) {
        clearTimeout(refreshTimeoutRef.current);
      }
    };
  }, [autoRefresh, refreshInterval, refreshUserInfo]);

  // Počáteční načtení při mount
  useEffect(() => {
    loadUserInfo(true);
  }, [loadUserInfo]);

  // Cleanup při unmount
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
      if (refreshTimeoutRef.current) {
        clearTimeout(refreshTimeoutRef.current);
      }
    };
  }, []);

  // Helper funkce pro získání specifických dat
  const getDisplayName = useCallback(() => {
    if (!userInfo) return 'Uživatel';
    
    if (userInfo.firstName && userInfo.lastName) {
      return `${userInfo.firstName} ${userInfo.lastName}`;
    }
    
    return userInfo.name || userInfo.username || 'Uživatel';
  }, [userInfo]);

  const hasRole = useCallback((role) => {
    return userInfo?.roles?.includes(role) || false;
  }, [userInfo]);

  const isAdmin = useCallback(() => {
    return hasRole('admin') || hasRole('administrator');
  }, [hasRole]);

  return {
    // Data
    userInfo,
    loading,
    error,
    lastUpdated,
    
    // Actions
    refreshUserInfo,
    
    // Helper functions
    getDisplayName,
    hasRole,
    isAdmin,
    
    // Computed values
    isAuthenticated: !!userInfo,
    tokenExpiresIn: userInfo?.tokenExpiresAt ? 
      Math.max(0, Math.floor((userInfo.tokenExpiresAt - new Date()) / 1000)) : null
  };
};