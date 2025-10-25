/**
 * Build Grafana iframe URL with proper query parameter merging
 * 
 * @param baseSubPath - Grafana subpath prefix (e.g., '/core-admin/monitoring')
 * @param path - Dashboard path (e.g., '/d/abc' or '/d/abc?var_x=1')
 * @param orgId - DEPRECATED: Grafana organization ID is now set server-side via setUserActiveOrg
 * @param defaults - Default query params (theme, kiosk)
 * @returns Relative URL with properly merged query params
 */
export function buildGrafanaUrl(
  baseSubPath: string,
  path: string,
  _orgId: number, // ⚠️ DEPRECATED but kept for API compatibility (prefixed with _ to avoid unused warning)
  defaults?: { theme?: 'light' | 'dark'; kiosk?: boolean }
): string {
  // Normalize path - remove leading slash
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path;
  
  // Build full URL using window.location.origin for proper URL parsing
  const fullPath = baseSubPath.replace(/\/$/, '') + '/' + normalizedPath;
  const url = new URL(fullPath, window.location.origin);
  
  // ❌ REMOVED: orgId is now set server-side via POST /api/users/{userId}/using/{orgId}
  // Backend AuthRequestController.ensureActiveOrg handles org selection
  // CRITICAL: Remove orgId from URL even if it exists in input path
  url.searchParams.delete('orgId');
  
  // Set theme if not already in query
  if (defaults?.theme && !url.searchParams.has('theme')) {
    url.searchParams.set('theme', defaults.theme);
  }
  
  // Set kiosk if not already in query
  if (defaults?.kiosk && !url.searchParams.has('kiosk')) {
    url.searchParams.set('kiosk', '1');
  }
  
  // Return relative URL (pathname + search)
  return url.pathname + url.search;
}
