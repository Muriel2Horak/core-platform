/**
 * 🏢 Tenant Utils - Core Platform Tenant Extraction
 * 
 * Utility funkce pro extrakci tenant informací z JWT tokenů.
 * Používá regex pattern `/realms/([^/]+)` pro parsing issuer URL.
 */

/**
 * Extrahuje tenant key z JWT issuer URL
 * 
 * @param issuer - JWT issuer URL (např. "https://admin.core-platform.local/realms/admin")
 * @returns tenant key nebo null při chybě
 */
export const tenantFromIssuer = (issuer: string): string | null => {
  if (!issuer || typeof issuer !== 'string') {
    return null;
  }

  try {
    // Regex pattern pro extrakci realm z issuer URL
    const realmMatch = issuer.match(/\/realms\/([^/]+)\/?$/);
    
    if (realmMatch && realmMatch[1]) {
      const tenantKey = realmMatch[1].trim();
      
      // Validace - tenant key nesmí být prázdný
      if (tenantKey.length === 0) {
        return null;
      }
      
      return tenantKey;
    }
    
    return null;
  } catch (error) {
    console.error('Error parsing tenant from issuer:', error);
    return null;
  }
};

/**
 * Extrahuje tenant informace z JWT tokenu
 * 
 * @param token - Parsed JWT token object
 * @returns tenant informace nebo fallback
 */
export const extractTenantInfo = (token: {
  tenant?: string;
  tenant_name?: string;
  iss?: string;
  [key: string]: unknown;
}): { key: string; name?: string } => {
  // Pokus o direct tenant claim
  if (token?.tenant) {
    return {
      key: token.tenant,
      name: token.tenant_name || token.tenant,
    };
  }
  
  // Fallback na issuer parsing
  if (token?.iss) {
    const tenantKey = tenantFromIssuer(token.iss);
    if (tenantKey) {
      return {
        key: tenantKey,
        name: tenantKey,
      };
    }
  }
  
  // Ultimate fallback - nemělo by se stát v produkci
  return {
    key: 'unknown',
    name: 'Unknown Tenant',
  };
};

/**
 * Validuje tenant key podle Core Platform pravidel
 * 
 * @param tenantKey - tenant key k validaci
 * @returns true pokud je valid
 */
export const isValidTenantKey = (tenantKey: string): boolean => {
  if (!tenantKey || typeof tenantKey !== 'string') {
    return false;
  }
  
  // Tenant key rules: pouze malá písmena, číslice, pomlčky
  // Musí začínat písmenem, minimálně 2 znaky
  const tenantKeyRegex = /^[a-z][a-z0-9-]*$/;
  
  return tenantKeyRegex.test(tenantKey) && tenantKey.length >= 2;
};

export default {
  tenantFromIssuer,
  extractTenantInfo,
  isValidTenantKey,
};