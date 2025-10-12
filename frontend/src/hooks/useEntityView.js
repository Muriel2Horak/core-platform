/**
 * EntityView SDK - React hooks for entity CRUD operations
 * 
 * @module hooks/useEntityView
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { apiClient } from '../services/api';

/**
 * Hook for fetching and managing a single entity
 * 
 * @param {string} entityType - Entity type (e.g., 'User', 'Order')
 * @param {string} entityId - Entity ID
 * @param {object} options - Configuration options
 * @param {boolean} options.enabled - Whether to auto-fetch (default: true)
 * @param {number} options.refetchInterval - Auto-refetch interval in ms
 * @param {function} options.onSuccess - Success callback
 * @param {function} options.onError - Error callback
 * @returns {object} Entity state and actions
 * 
 * @example
 * const { data, loading, error, refetch } = useEntityView('User', userId);
 */
export function useEntityView(entityType, entityId, options = {}) {
  const {
    enabled = true,
    refetchInterval,
    onSuccess,
    onError,
  } = options;

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);

  const fetchEntity = useCallback(async () => {
    if (!entityId || !enabled) return;

    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.get(`/api/entities/${entityType}/${entityId}`);
      setData(response.data);
      onSuccess?.(response.data);
    } catch (err) {
      setError(err);
      onError?.(err);
    } finally {
      setLoading(false);
    }
  }, [entityType, entityId, enabled, onSuccess, onError]);

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
    refetch: fetchEntity,
    isSuccess: !loading && !error && data !== null,
    isError: !loading && error !== null,
  };
}

/**
 * Hook for entity mutations (create, update, delete)
 * 
 * @param {string} entityType - Entity type
 * @returns {object} Mutation functions and state
 * 
 * @example
 * const { create, update, remove, loading } = useEntityMutation('User');
 * await create({ name: 'John', email: 'john@example.com' });
 */
export function useEntityMutation(entityType) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

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

  const update = useCallback(async (entityId, data) => {
    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.put(`/api/entities/${entityType}/${entityId}`, data);
      return response.data;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType]);

  const remove = useCallback(async (entityId) => {
    setLoading(true);
    setError(null);

    try {
      await apiClient.delete(`/api/entities/${entityType}/${entityId}`);
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType]);

  const patch = useCallback(async (entityId, data) => {
    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.patch(`/api/entities/${entityType}/${entityId}`, data);
      return response.data;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [entityType]);

  return {
    create,
    update,
    remove,
    patch,
    loading,
    error,
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
