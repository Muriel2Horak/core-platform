import { useEffect, useRef, useState, useCallback } from 'react';
import { PresenceClient, PresenceConfig } from './PresenceClient';

export interface PresenceState {
  users: string[];
  stale: boolean;
  busyBy: string | null;
  version: number | null;
  connected: boolean;
}

export interface UsePresenceOptions {
  enabled?: boolean;
  wsUrl?: string; // Default: ws://localhost:8080/ws/presence
}

/**
 * React hook for real-time presence tracking
 * 
 * Usage:
 * ```tsx
 * const { presence, acquireLock, releaseLock, error } = usePresence({
 *   entity: 'Order',
 *   id: orderId,
 *   tenantId: currentTenant,
 *   userId: currentUser,
 * });
 * 
 * // Display presence
 * <div>
 *   {presence.users.length} users viewing
 *   {presence.stale && <Badge>Editing by {presence.busyBy}</Badge>}
 * </div>
 * 
 * // Acquire lock on field
 * const handleFieldFocus = (field: string) => {
 *   acquireLock(field);
 * };
 * ```
 */
export function usePresence(
  config: {
    entity: string;
    id: string;
    tenantId: string;
    userId: string;
  },
  options: UsePresenceOptions = {}
) {
  const { enabled = true, wsUrl = 'ws://localhost:8080/ws/presence' } = options;

  const [presence, setPresence] = useState<PresenceState>({
    users: [],
    stale: false,
    busyBy: null,
    version: null,
    connected: false,
  });

  const [error, setError] = useState<string | null>(null);
  const [lockResults, setLockResults] = useState<Map<string, boolean>>(new Map());

  const clientRef = useRef<PresenceClient | null>(null);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    const presenceConfig: PresenceConfig = {
      url: wsUrl,
      userId: config.userId,
      tenantId: config.tenantId,
      entity: config.entity,
      id: config.id,
      onPresenceUpdate: (users, stale, busyBy, version) => {
        setPresence((prev) => ({
          ...prev,
          users,
          stale,
          busyBy,
          version,
          connected: true,
        }));
        setError(null);
      },
      onLockResult: (field, success) => {
        setLockResults((prev) => new Map(prev).set(field, success));
      },
      onError: (err) => {
        setError(err);
        setPresence((prev) => ({ ...prev, connected: false }));
      },
    };

    const client = new PresenceClient(presenceConfig);
    clientRef.current = client;
    client.connect();

    return () => {
      client.disconnect();
      clientRef.current = null;
    };
  }, [enabled, wsUrl, config.userId, config.tenantId, config.entity, config.id]);

  const acquireLock = useCallback((field: string) => {
    clientRef.current?.acquireLock(field);
  }, []);

  const releaseLock = useCallback((field: string) => {
    clientRef.current?.releaseLock(field);
  }, []);

  const getLockStatus = useCallback((field: string): boolean | undefined => {
    return lockResults.get(field);
  }, [lockResults]);

  return {
    presence,
    error,
    acquireLock,
    releaseLock,
    getLockStatus,
  };
}
