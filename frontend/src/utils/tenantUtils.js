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
    const match = iss.match(/\/realms\/([^\/]+)/);
    return match ? match[1] : null;
  } catch (error) {
    console.error('Failed to extract realm from issuer:', error);
    return null;
  }
}