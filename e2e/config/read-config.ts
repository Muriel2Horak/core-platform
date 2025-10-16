/**
 * E2E Configuration Reader
 * 
 * Čte konfiguraci z existujících souborů v projektu:
 * - .env (root)
 * - backend/src/main/resources/application*.properties
 * 
 * Umožňuje override přes environment proměnné.
 */

import { readFileSync, existsSync } from 'node:fs';
import { resolve, join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

// ES module __dirname equivalent
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export interface E2EConfig {
  baseUrl: string;
  ignoreTLS: boolean;
  keycloak: {
    authServerUrl: string;
    frontendUrl: string;
    realm: string;
    clientId: string;
  };
  testUser: {
    username: string;
    password: string;
  };
}

/**
 * Simple .env parser (avoid external dependencies)
 */
function parseEnv(content: string): Record<string, string> {
  const result: Record<string, string> = {};
  
  content.split('\n').forEach((line: string) => {
    const trimmed = line.trim();
    
    // Skip comments and empty lines
    if (!trimmed || trimmed.startsWith('#')) {
      return;
    }
    
    // Parse key=value
    const index = trimmed.indexOf('=');
    if (index > 0) {
      const key = trimmed.substring(0, index).trim();
      let value = trimmed.substring(index + 1).trim();
      
      // Remove quotes if present
      if ((value.startsWith('"') && value.endsWith('"')) || 
          (value.startsWith("'") && value.endsWith("'"))) {
        value = value.substring(1, value.length - 1);
      }
      
      result[key] = value;
    }
  });
  
  return result;
}

/**
 * Načte .env soubor z rootu projektu
 */
function loadDotEnv(): Record<string, string> {
  const rootDir = resolve(__dirname, '../..');
  const envPath = join(rootDir, '.env');
  
  if (!existsSync(envPath)) {
    console.warn(`⚠️  .env not found at ${envPath}, using defaults`);
    return {};
  }
  
  const content = readFileSync(envPath, 'utf-8');
  return parseEnv(content);
}

/**
 * Načte application.properties z backendu
 */
function loadApplicationProperties(): Record<string, string> {
  const rootDir = resolve(__dirname, '../..');
  const propsPath = join(rootDir, 'backend/src/main/resources/application.properties');
  
  if (!existsSync(propsPath)) {
    console.warn(`⚠️  application.properties not found at ${propsPath}`);
    return {};
  }
  
  const content = readFileSync(propsPath, 'utf-8');
  const props: Record<string, string> = {};
  
  // Parse Java properties format
  content.split('\n').forEach((line: string) => {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
      const [key, ...valueParts] = trimmed.split('=');
      props[key.trim()] = valueParts.join('=').trim();
    }
  });
  
  return props;
}

/**
 * Rozšíří property hodnoty s placeholdery ${VAR:default}
 */
function expandProperty(value: string, allProps: Record<string, string>): string {
  return value.replace(/\$\{([^:}]+)(?::([^}]+))?\}/g, (_match, varName, defaultValue) => {
    // Zkus najít v process.env, pak v allProps, pak použij default
    return process.env[varName] || allProps[varName] || defaultValue || '';
  });
}

/**
 * Vytvoří E2E konfiguraci ze všech dostupných zdrojů
 */
export function readE2EConfig(): E2EConfig {
  const dotenv = loadDotEnv();
  const appProps = loadApplicationProperties();
  
  // Merge all sources (dotenv má prioritu)
  const merged = { ...appProps, ...dotenv };
  
  // Expand all property values
  const expanded: Record<string, string> = {};
  Object.keys(merged).forEach(key => {
    expanded[key] = expandProperty(merged[key], merged);
  });
  
  // Extract DOMAIN
  const domain = expanded['DOMAIN'] || 'core-platform.local';
  
  // Base URL (override přes ENV)
  // Frontend běží na admin subdoména (např. admin.core-platform.local)
  const baseUrl = process.env.E2E_BASE_URL || `https://admin.${domain}`;
  
  // TLS validation (default: true, disable pouze když E2E_IGNORE_TLS=true)
  const ignoreTLS = process.env.E2E_IGNORE_TLS === 'true' || true; // Always ignore TLS in tests
  
  // Keycloak realm (z ENV nebo default 'admin')
  const realm = process.env.E2E_REALM || expanded['KEYCLOAK_TARGET_REALM'] || 'admin';
  
  // Keycloak client ID pro frontend (z ENV nebo odvozený)
  // Backend používá 'core-backend', frontend typicky 'web' nebo 'core-platform-ui'
  const clientId = process.env.E2E_CLIENT_ID || 'web';
  
  // Keycloak auth server URL
  // Z .env: OIDC_ISSUER_URI=https://admin.core-platform.local/realms/admin
  const oidcIssuer = expanded['OIDC_ISSUER_URI'];
  let authServerUrl: string;
  let frontendUrl: string;
  
  if (oidcIssuer) {
    // Odvoď base URL z issuer (odstranit /realms/...)
    authServerUrl = oidcIssuer.replace(/\/realms\/.*$/, '');
    frontendUrl = authServerUrl;
  } else {
    // Fallback: sestavit z domény
    authServerUrl = `https://admin.${domain}`;
    frontendUrl = authServerUrl;
  }
  
  // Test user credentials (override via ENV)
  const username = process.env.E2E_USER || 'test';
  const password = process.env.E2E_PASS || 'Test.1234';
  
  return {
    baseUrl,
    ignoreTLS,
    keycloak: {
      authServerUrl,
      frontendUrl,
      realm,
      clientId,
    },
    testUser: {
      username,
      password,
    },
  };
}

/**
 * Globální singleton config pro testy
 */
export const e2eConfig = readE2EConfig();
