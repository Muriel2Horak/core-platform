/**
 * 🏢 Tenant utilities - extraction from Keycloak issuer URLs
 */

/**
 * Extracts realm/tenant name from Keycloak issuer URL
 * @param {string} iss - Keycloak issuer URL (e.g., "https://auth.example.com/realms/tenant-name")
 * @returns {string|null} - Tenant name or null if extraction fails
 */
export function realmFromIss(iss) {
  if (!iss || typeof iss !== 'string') {
    return null;
  }

  try {
    // Extract realm from issuer URL pattern: /realms/{realm-name}
    const match = iss.match(/\/realms\/([^/]+)/);
    return match ? match[1] : null;
  } catch (error) {
    console.error('Failed to extract realm from issuer:', error);
    return null;
  }
}

/**
 * Extract tenant from hostname or URL path
 * @param {string} hostname - Current hostname
 * @param {string} path - URL path
 * @returns {string|null} - Tenant name or null if extraction fails
 */
export function extractTenantFromUrl(hostname, path) {
  // Extract subdomain from hostname (e.g., "tenant1" from "tenant1.core-platform.local")
  if (hostname && hostname.includes('.')) {
    const parts = hostname.split('.');
    if (parts.length >= 3) {
      const subdomain = parts[0];
      console.log('🎯 Extracted tenant from subdomain', { 
        hostname, 
        subdomain, 
        parts 
      });
      return subdomain;
    }
  }

  // Extract from URL path format: /tenant/{tenantName}/...
  if (path) {
    const pathMatch = path.match(/^\/tenant\/([^/]+)/);
    if (pathMatch) {
      const tenant = pathMatch[1];
      console.log('🎯 Extracted tenant from URL path', { 
        path, 
        tenant 
      });
      return tenant;
    }
  }

  return null;
}