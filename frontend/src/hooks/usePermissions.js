import { useState, useEffect, useCallback } from 'react';
import apiService from '../services/api';
import logger from '../services/logger';

/**
 * 🔐 usePermissions Hook v2.0
 * 
 * ZMĚNY (RBAC→ABAC refactor):
 * - Volá GET /api/me/ui-capabilities místo /api/permissions/me
 * - Podporuje ETag caching (If-None-Match header)
 * - JWT zůstává krátký (roles + tenant + perm_version)
 * - UI capabilities se načítají ze serveru (menu + features)
 */
export const usePermissions = () => {
  const [capabilities, setCapabilities] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchCapabilities = useCallback(async (force = false) => {
    try {
      setLoading(true);
      
      // Získat cached ETag z localStorage
      const cachedETag = force ? null : localStorage.getItem('permETag');
      const cachedData = force ? null : localStorage.getItem('permCapabilities');
      
      const headers = {};
      if (cachedETag && cachedData) {
        headers['If-None-Match'] = cachedETag;
      }

      const response = await apiService.get('/api/me/ui-capabilities', { headers });
      
      // 304 Not Modified - použít cached data
      if (response.status === 304 && cachedData) {
        logger.debug('UI capabilities unchanged (304), using cache', { etag: cachedETag });
        setCapabilities(JSON.parse(cachedData));
      } 
      // 200 OK - nová data
      else if (response.data) {
        const data = response.data;
        const newETag = response.headers?.etag;
        
        setCapabilities(data);
        
        // Uložit do localStorage
        if (newETag) {
          localStorage.setItem('permETag', newETag);
          localStorage.setItem('permCapabilities', JSON.stringify(data));
          logger.debug('UI capabilities loaded and cached', { 
            etag: newETag,
            menuItems: data.menu?.length,
            features: data.features?.length,
            dataScope: data.dataScope
          });
        }
      }
    } catch (err) {
      // 403 Forbidden - permissions se změnily, zkusit znovu
      if (err.response?.status === 403 && !force) {
        logger.warn('403 received, re-fetching capabilities (permissions changed)');
        localStorage.removeItem('permETag');
        localStorage.removeItem('permCapabilities');
        return fetchCapabilities(true);
      }
      
      logger.error('Failed to load UI capabilities', err);
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCapabilities();
  }, [fetchCapabilities]);

  /**
   * Zkontroluje API permission
   * 
   * DEPRECATED: V2.0 používáme hasFeature() pro UI control.
   * Backend vynucuje real permissions přes @PreAuthorize.
   */
  const can = useCallback(() => {
    // TODO: V budoucnu odstranit, FE by neměl kontrolovat API permissions
    // Pouze backend má autoritativní source přes PolicyEngine
    logger.warn('can() is deprecated - use hasFeature() for UI control');
    return false;
  }, []);

  /**
   * Helper pro běžné CRUD operace
   * DEPRECATED: Použij hasFeature() místo canRead/canCreate/etc.
   */
  const canRead = useCallback((resource, scope = 'all') => {
    return can(`${resource}:read:${scope}`);
  }, [can]);

  const canCreate = useCallback((resource, scope = 'all') => {
    return can(`${resource}:create:${scope}`);
  }, [can]);

  const canUpdate = useCallback((resource, scope = 'all') => {
    return can(`${resource}:update:${scope}`);
  }, [can]);

  const canDelete = useCallback((resource, scope = 'all') => {
    return can(`${resource}:delete:${scope}`);
  }, [can]);

  /**
   * Zkontroluje, zda má přístup k menu položce
   * @param {string} menuId - ID menu item z capabilities.menu
   */
  const hasMenu = useCallback((menuId) => {
    if (!capabilities?.menu) return false;
    return capabilities.menu.some(item => item.id === menuId);
  }, [capabilities]);

  /**
   * Zkontroluje, zda má feature
   * @param {string} feature - např. "grafana_admin", "export_data"
   */
  const hasFeature = useCallback((feature) => {
    if (!capabilities?.features) return false;
    return capabilities.features.includes(feature);
  }, [capabilities]);

  /**
   * Získá data scope uživatele
   * @returns {string} "all_tenants" | "own_tenant" | "own_data"
   */
  const getDataScope = useCallback(() => {
    return capabilities?.dataScope || 'own_data';
  }, [capabilities]);

  /**
   * Získá seznam menu items
   */
  const getMenuItems = useCallback(() => {
    return capabilities?.menu || [];
  }, [capabilities]);

  return {
    // V2.0 API
    capabilities,
    loading,
    error,
    hasMenu,
    hasFeature,
    getDataScope,
    getMenuItems,
    refreshCapabilities: () => fetchCapabilities(true),
    
    // V1.0 API (deprecated, backward compatibility)
    permissions: capabilities,
    can,
    canRead,
    canCreate,
    canUpdate,
    canDelete,
  };
};

/**
 * Higher-order component pro permission check
 */
export const withPermission = (Component, requiredPermission) => {
  return (props) => {
    const { can, loading } = usePermissions();

    if (loading) {
      return <div>Načítání oprávnění...</div>;
    }

    if (!can(requiredPermission)) {
      return <div>Nemáte oprávnění k zobrazení této stránky.</div>;
    }

    return <Component {...props} />;
  };
};
