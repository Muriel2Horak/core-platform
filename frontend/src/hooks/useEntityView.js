/**
 * EntityView SDK - React hooks for entity CRUD operations
 * With Presence, Locks, and ETag support
 * 
 * @module hooks/useEntityView
 */

import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { apiClient } from '../services/api';
import { usePresence } from '../lib/presence/usePresence';

/**
 * Hook for fetching and managing a single entity
 * WITH PRESENCE TRACKING AND ETAG SUPPORT
 * 
 * @param {string} entityType - Entity type (e.g., 'User', 'Order')
 * @param {string} entityId - Entity ID
 * @param {object} options - Configuration options
 * @param {boolean} options.enabled - Whether to auto-fetch (default: true)
 * @param {number} options.refetchInterval - Auto-refetch interval in ms
 * @param {function} options.onSuccess - Success callback
 * @param {function} options.onError - Error callback
 * @param {boolean} options.trackPresence - Enable presence tracking (default: true)
 * @param {boolean} options.useETag - Enable ETag validation (default: true)
 * @returns {object} Entity state and actions
 * 
 * @example
 * const { data, loading, error, refetch, etag, presence } = useEntityView('User', userId);
 * // presence: { users: [], locks: {}, stale: false, version: 123 }
 * // etag: 'W/"abc123"' - for conflict detection
 */
export function useEntityView(entityType, entityId, options = {}) {
  const {
    enabled = true,
    refetchInterval,
    onSuccess,
    onError,
    trackPresence = true,
    useETag = true,
  } = options;

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);
  const [etag, setETag] = useState(null);
  const etagRef = useRef(null);

  // Presence tracking for this entity
  const presence = usePresence(
    trackPresence ? entityType : null,
    trackPresence ? entityId : null,
    { enabled: trackPresence && !!entityId }
  );

  const fetchEntity = useCallback(async () => {
    if (!entityId || !enabled) return;

    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.get(`/api/entities/${entityType}/${entityId}`);
      
      setData(response.data);
      
      // Store ETag from response headers
      if (useETag && response.headers?.etag) {
        const etagValue = response.headers.etag;
        setETag(etagValue);
        etagRef.current = etagValue;
      }
      
      onSuccess?.(response.data);
    } catch (err) {
      setError(err);
      onError?.(err);
    } finally {
      setLoading(false);
    }
  }, [entityType, entityId, enabled, onSuccess, onError, useETag]);

  useEffect(() => {
    fetchEntity();
  }, [fetchEntity]);

  useEffect(() => {
    if (refetchInterval && enabled) {
      const interval = setInterval(fetchEntity, refetchInterval);
      return () => clearInterval(interval);
    }
  }, [fetchEntity, refetchInterval, enabled]);

  return {
    data,
    loading,
    error,
    etag,
    presence: trackPresence ? presence : null,
    refetch: fetchEntity,
    isSuccess: !loading && !error && data !== null,
    isError: !loading && error !== null,
    // Helper to check if entity is locked by someone else
    isLockedByOthers: trackPresence 
      ? Object.keys(presence?.locks || {}).some(userId => userId !== presence?.currentUserId)
      : false,
    // Helper to check if current user has any locks
    hasLocks: trackPresence
      ? Object.keys(presence?.locks || {}).some(userId => userId === presence?.currentUserId)
      : false,
  };
}

/**
 * Hook for entity mutations (create, update, delete)
 * WITH FIELD-LEVEL LOCKING AND ETAG CONFLICT DETECTION
 * 
 * @param {string} entityType - Entity type
 * @param {object} options - Mutation options
 * @param {string} options.entityId - Entity ID (for presence tracking)
 * @param {string} options.etag - Current ETag (for conflict detection)
 * @param {function} options.onConflict - Callback for 409/412 conflicts
 * @returns {object} Mutation functions and state
 * 
 * @example
 * const { patch, loading, lockField, unlockField } = useEntityMutation('User', {
 *   entityId: userId,
 *   etag: currentEtag,
 *   onConflict: ({ error, reload }) => {
 *     if (confirm('Data changed. Reload?')) reload();
 *   }
 * });
 * 
 * // Lock field before editing
 * await lockField('email');
 * await patch({ email: 'new@email.com' });
 * await unlockField('email');
 */
export function useEntityMutation(entityType, options = {}) {
  const { entityId, etag, onConflict } = options;
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Presence for field-level locking
  const presence = usePresence(
    entityId ? entityType : null,
    entityId,
    { enabled: !!entityId }
  );

  const create = useCallback(async (data) => {
    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.post(`/api/entities/${entityType}`, data);
      return response.data;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType]);

  const update = useCallback(async (id, data) => {
    setLoading(true);
    setError(null);

    try {
      // Add If-Match header for ETag validation
      const headers = etag ? { 'If-Match': etag } : {};
      
      const response = await apiClient.put(
        `/api/entities/${entityType}/${id}`, 
        data,
        { headers }
      );
      return response.data;
    } catch (err) {
      // Handle 409 Conflict / 412 Precondition Failed
      if ((err.response?.status === 409 || err.response?.status === 412) && onConflict) {
        onConflict({
          error: err,
          status: err.response.status,
          message: err.response?.data?.message || 'Data has been modified by another user',
          reload: async () => {
            // Caller should refetch entity
            throw new Error('CONFLICT_RELOAD_NEEDED');
          },
        });
      }
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType, etag, onConflict]);

  const remove = useCallback(async (id) => {
    setLoading(true);
    setError(null);

    try {
      await apiClient.delete(`/api/entities/${entityType}/${id}`);
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType]);

  const patch = useCallback(async (id, data, fieldName = null) => {
    setLoading(true);
    setError(null);

    try {
      // Check if field is locked by someone else
      if (fieldName && presence?.locks) {
        const fieldLocks = Object.entries(presence.locks)
          .filter(([userId, fields]) => 
            fields.includes(fieldName) && userId !== presence.currentUserId
          );
        
        if (fieldLocks.length > 0) {
          const lockedBy = fieldLocks[0][0];
          throw new Error(`Field "${fieldName}" is locked by user ${lockedBy}`);
        }
      }
      
      // Add If-Match header for ETag validation
      const headers = etag ? { 'If-Match': etag } : {};
      
      const response = await apiClient.patch(
        `/api/entities/${entityType}/${id}`, 
        data,
        { headers }
      );
      return response.data;
    } catch (err) {
      // Handle 409 Conflict / 412 Precondition Failed
      if ((err.response?.status === 409 || err.response?.status === 412) && onConflict) {
        onConflict({
          error: err,
          status: err.response.status,
          message: err.response?.data?.message || 'Data has been modified by another user',
          reload: async () => {
            throw new Error('CONFLICT_RELOAD_NEEDED');
          },
        });
      }
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType, etag, onConflict, presence]);

  // Field-level lock/unlock helpers
  const lockField = useCallback(async (fieldName) => {
    if (!presence?.lock) {
      console.warn('Presence not available for field locking');
      return false;
    }
    return await presence.lock(fieldName);
  }, [presence]);

  const unlockField = useCallback(async (fieldName) => {
    if (!presence?.unlock) {
      console.warn('Presence not available for field unlocking');
      return false;
    }
    return await presence.unlock(fieldName);
  }, [presence]);

  return {
    create,
    update,
    remove,
    patch,
    lockField,
    unlockField,
    loading,
    error,
    presence,
  };
}

/**
 * Hook for fetching a list of entities with filtering and pagination
 * 
 * @param {string} entityType - Entity type
 * @param {object} options - Query options
 * @param {object} options.filters - Filter criteria
 * @param {number} options.page - Page number (0-indexed)
 * @param {number} options.pageSize - Items per page
 * @param {string} options.sortBy - Sort field
 * @param {string} options.sortOrder - Sort order ('asc' or 'desc')
 * @param {boolean} options.enabled - Whether to auto-fetch (default: true)
 * @returns {object} List state and actions
 * 
 * @example
 * const { data, loading, refetch, pagination } = useEntityList('User', {
 *   filters: { status: 'active' },
 *   page: 0,
 *   pageSize: 20,
 *   sortBy: 'createdAt',
 *   sortOrder: 'desc'
 * });
 */
export function useEntityList(entityType, options = {}) {
  const {
    filters = {},
    page = 0,
    pageSize = 20,
    sortBy,
    sortOrder = 'asc',
    enabled = true,
    onSuccess,
    onError,
  } = options;

  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page,
    pageSize,
    totalElements: 0,
    totalPages: 0,
  });

  const queryParams = useMemo(() => {
    const params = new URLSearchParams();
    
    params.append('page', page);
    params.append('size', pageSize);
    
    if (sortBy) {
      params.append('sort', `${sortBy},${sortOrder}`);
    }
    
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params.append(key, value);
      }
    });
    
    return params;
  }, [filters, page, pageSize, sortBy, sortOrder]);

  const fetchList = useCallback(async () => {
    if (!enabled) return;

    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.get(`/api/entities/${entityType}?${queryParams}`);
      setData(response.data.content || response.data);
      
      if (response.data.totalElements !== undefined) {
        setPagination({
          page: response.data.number,
          pageSize: response.data.size,
          totalElements: response.data.totalElements,
          totalPages: response.data.totalPages,
        });
      }
      
      onSuccess?.(response.data);
    } catch (err) {
      setError(err);
      onError?.(err);
    } finally {
      setLoading(false);
    }
  }, [entityType, queryParams, enabled, onSuccess, onError]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  return {
    data,
    loading,
    error,
    pagination,
    refetch: fetchList,
    isSuccess: !loading && !error,
    isError: !loading && error !== null,
  };
}

/**
 * Hook for optimistic entity updates with rollback
 * 
 * @param {string} entityType - Entity type
 * @param {string} entityId - Entity ID
 * @returns {object} Optimistic update actions
 * 
 * @example
 * const { updateOptimistic, rollback } = useOptimisticUpdate('User', userId);
 * updateOptimistic({ name: 'New Name' });
 */
export function useOptimisticUpdate(entityType, entityId) {
  const [optimisticData, setOptimisticData] = useState(null);
  const [originalData, setOriginalData] = useState(null);

  const updateOptimistic = useCallback((updates) => {
    setOriginalData((prev) => prev || optimisticData);
    setOptimisticData((prev) => ({ ...prev, ...updates }));
  }, [optimisticData]);

  const rollback = useCallback(() => {
    if (originalData) {
      setOptimisticData(originalData);
      setOriginalData(null);
    }
  }, [originalData]);

  const commit = useCallback(async () => {
    if (!optimisticData) return;

    try {
      const response = await apiClient.put(`/api/entities/${entityType}/${entityId}`, optimisticData);
      setOriginalData(null);
      return response.data;
    } catch (err) {
      rollback();
      throw err;
    }
  }, [entityType, entityId, optimisticData, rollback]);

  return {
    data: optimisticData,
    updateOptimistic,
    rollback,
    commit,
    isDirty: originalData !== null,
  };
}
