import { useState, useEffect } from 'react';

interface UserProfile {
  grafanaOrgId?: number;
  [key: string]: any;
}

interface UseGrafanaOrgIdResult {
  orgId: number | null;
  loading: boolean;
  error: Error | null;
}

// Session cache to avoid repeated API calls
let cachedOrgId: number | null = null;
let cachePromise: Promise<number | null> | null = null;

/**
 * Hook to fetch Grafana orgId from /api/me endpoint
 * 
 * Uses session-level caching to avoid repeated API calls.
 * The orgId is the source of truth for all Grafana embeds.
 */
export function useGrafanaOrgId(): UseGrafanaOrgIdResult {
  const [orgId, setOrgId] = useState<number | null>(cachedOrgId);
  const [loading, setLoading] = useState<boolean>(cachedOrgId === null);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // If already cached, use it
    if (cachedOrgId !== null) {
      setOrgId(cachedOrgId);
      setLoading(false);
      return;
    }

    // If fetch is already in progress, reuse the promise
    if (cachePromise) {
      cachePromise
        .then(id => {
          setOrgId(id);
          setLoading(false);
        })
        .catch(err => {
          setError(err);
          setLoading(false);
        });
      return;
    }

    // Start new fetch
    setLoading(true);
    cachePromise = fetch('/api/me', {
      credentials: 'include', // Include cookies
      headers: {
        'Accept': 'application/json',
      },
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`Failed to fetch user profile: ${response.status} ${response.statusText}`);
        }
        return response.json() as Promise<UserProfile>;
      })
      .then(profile => {
        if (profile.grafanaOrgId === undefined || profile.grafanaOrgId === null) {
          throw new Error('grafanaOrgId not found in user profile');
        }
        cachedOrgId = profile.grafanaOrgId;
        return cachedOrgId;
      })
      .catch(err => {
        cachePromise = null; // Clear promise on error to allow retry
        throw err;
      });

    cachePromise
      .then(id => {
        setOrgId(id);
        setLoading(false);
      })
      .catch(err => {
        setError(err instanceof Error ? err : new Error(String(err)));
        setLoading(false);
      });
  }, []);

  return { orgId, loading, error };
}

/**
 * Reset cache (for testing or logout)
 */
export function resetGrafanaOrgIdCache(): void {
  cachedOrgId = null;
  cachePromise = null;
}
