// Keycloak service with auto-detection from hostname
import Keycloak from 'keycloak-js';
import logger from './logger.js';

// 🔧 MODULE-LEVEL SINGLETON PATTERN - prevence duplicitní inicializace
let initPromise = null;
let keycloakInstance = null;
let isInitialized = false;

class KeycloakService {
  constructor() {
    this.keycloak = null;
    this.initialized = false;
    this.config = this.getKeycloakConfig();
  }

  /**
   * 🌐 Automatic Keycloak configuration detection
   */
  getKeycloakConfig() {
    const hostname = window.location.hostname;
    const protocol = window.location.protocol;

    // 🔧 FIXED: Use same logic as backend - extract tenant from hostname with fallback
    const realm = this.extractRealmFromHostname(hostname);
    
    // 🔧 FIXED: Build correct Keycloak URL based on hostname (realm not needed for URL)
    const keycloakUrl = this.buildKeycloakUrl(hostname);
    
    logger.debug('Using realm from hostname', { 
      hostname, 
      realm,
      keycloakUrl,
      component: 'auth' 
    });

    logger.info('Keycloak config detected', { 
      keycloakUrl, 
      realm, 
      clientId: 'web',
      component: 'auth' 
    });

    return {
      url: keycloakUrl,
      realm: realm,
      clientId: 'web',

      // Debug info
      _debug: {
        hostname,
        protocol,
        realmFromHostname: realm,
        keycloakUrl,
        usingSSL: true,
        usingNginxProxy: true
      }
    };
  }

  /**
   * 🔧 BUILD KEYCLOAK URL: Build correct Keycloak URL
   * Uses nginx proxy, so realm is not needed in URL
   */
  buildKeycloakUrl(hostname) {
    // 🔧 FIXED: Use relative URL to leverage nginx proxy
    // Nginx routes /realms/* and /admin/* to keycloak service
    const protocol = window.location.protocol;
    const port = window.location.port ? `:${window.location.port}` : '';
    
    // Use current origin to go through nginx proxy
    return `${protocol}//${hostname}${port}`;
  }

  /**
   * 🌐 EXTRACT REALM FROM HOSTNAME: Unified tenant extraction (same logic as backend)
   */
  extractRealmFromHostname(hostname) {
    if (!hostname || hostname === '') {
      return 'admin'; // fallback
    }

    logger.debug('🌐 Processing hostname: ' + hostname);

    // Direct hostname mapping - unified logic for all tenants
    if (hostname.includes('.')) {
      const parts = hostname.split('.');
      if (parts.length >= 3) {
        const subdomain = parts[0];
        logger.debug('🎯 Extracted subdomain: ' + subdomain);
        return subdomain; // This will be "admin", "tenant1", "tenant2", etc.
      }
    }

    // Fallback for localhost or direct domain access (core-platform.local)
    return 'admin'; // fallback (same as backend)
  }

  /**
   * 🚀 SINGLETON INIT - Initialize Keycloak only once
   * Returns the same promise for all callers to prevent duplicate initialization
   */
  async initKeycloakOnce() {
    // 🔧 Return existing promise if initialization is in progress
    if (initPromise) {
      logger.debug('Keycloak initialization already in progress, returning existing promise');
      return initPromise;
    }

    // 🔧 Return existing instance if already initialized
    if (isInitialized && keycloakInstance) {
      logger.debug('Keycloak already initialized, returning existing instance');
      return keycloakInstance;
    }

    // 🔧 Create and store the initialization promise
    initPromise = this._doInitKeycloak();
    
    try {
      const result = await initPromise;
      isInitialized = true;
      keycloakInstance = result;
      return result;
    } catch (error) {
      // Reset promise on error to allow retry
      initPromise = null;
      isInitialized = false;
      keycloakInstance = null;
      throw error;
    }
  }

  /**
   * 🔧 Internal initialization method
   */
  async _doInitKeycloak() {
    try {
      logger.auth('Keycloak initialization started', { 
        config: this.config, 
        url: window.location.href 
      });

      // 🔧 Create new Keycloak instance
      this.keycloak = new Keycloak(this.config);

      // 🔧 Initialize with automatic login (login-required for SSO)
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',  // 🔧 Automaticky redirectne na Keycloak pokud není přihlášen
        checkLoginIframe: false,   // 🔧 VYPNUTO podle požadavku
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html` // 🔧 Explicit URI
      });

      if (authenticated) {
        const userInfo = {
          realm: this.keycloak.realm,
          username: this.keycloak.tokenParsed?.preferred_username,
          tenant: this.getTenant(),
          roles: this.keycloak.tokenParsed?.realm_access?.roles
        };

        logger.auth('Keycloak authentication successful', {
          ...userInfo,
          tokenLength: this.keycloak.token?.length
        });

        // Store token for API calls
        localStorage.setItem('keycloak-token', this.keycloak.token);

        // Setup token refresh
        this.setupTokenRefresh();
        
        // Set logger context and authentication status
        logger.setTenantContext(userInfo.tenant, userInfo.username);
        logger.setAuthenticated(true);

        this.initialized = true;
      } else {
        logger.auth('Keycloak check-sso completed - user not authenticated');
        logger.setAuthenticated(false);
        this.initialized = true;
      }

      return this.keycloak;
    } catch (error) {
      // 🔐 FIXED: Better error handling - check if error exists and has message
      const errorMessage = error?.message || 'Unknown initialization error';
      
      console.error('❌ [AUTH] Keycloak initialization failed:', errorMessage, error);
      logger.setAuthenticated(false);
      this.initialized = false;
      throw new Error(`Keycloak initialization failed: ${errorMessage}`);
    }
  }

  /**
   * 🚀 Legacy init method - now uses singleton
   * @deprecated Use initKeycloakOnce() instead
   */
  async init() {
    return this.initKeycloakOnce();
  }

  /**
   * 🎯 Get tenant from JWT token issuer
   */
  getTenant() {
    const iss = this.keycloak?.tokenParsed?.iss;
    
    if (iss) {
      // Extract realm from issuer URL like: https://admin.core-platform.local/auth/realms/admin
      const match = iss.match(/\/realms\/([^/?#]+)/);
      if (match) {
        const tenant = match[1];
        logger.info('Tenant derived from token issuer', { 
          tenant, 
          issuer: iss,
          component: 'auth' 
        });
        return tenant;
      }
    }

    logger.warn('Could not derive tenant from issuer. Fallback to admin', { 
      issuer: iss,
      component: 'auth' 
    });
    return 'admin';
  }

  /**
   * 🔄 Setup automatic token refresh
   */
  setupTokenRefresh() {
    if (!this.keycloak) return;

    const refreshInterval = setInterval(async () => {
      try {
        if (!this.keycloak.authenticated) {
          clearInterval(refreshInterval);
          return;
        }

        // Refresh token when close to expiration (60s before expiry)
        if (this.keycloak.isTokenExpired(60)) {
          logger.auth('Token is expiring, attempting refresh...');

          const refreshed = await this.keycloak.updateToken(60);
          if (refreshed) {
            logger.auth('Token refreshed successfully', {
              tokenLength: this.keycloak.token?.length
            });
            localStorage.setItem('keycloak-token', this.keycloak.token);

            // Notify about successful refresh
            window.dispatchEvent(new CustomEvent('keycloak-token-refreshed', {
              detail: { 
                token: this.keycloak.token,
                tenant: this.getTenant()
              }
            }));
          }
        }
      } catch (error) {
        logger.error('Token refresh failed', { 
          error: error.message,
          stack: error.stack,
          component: 'auth' 
        });

        if (this.keycloak.isTokenExpired(0)) {
          logger.auth('Token expired, logging out...');
          this.logout();
        }
      }
    }, 30000); // Check every 30 seconds

    this._refreshInterval = refreshInterval;
  }

  /**
   * 🔐 Redirect to Keycloak login
   */
  async login() {
    // 🔧 FIXED: Auto-initialize if not ready
    if (!this.keycloak || !isInitialized) {
      logger.info('⏳ Keycloak not ready for login, initializing...');
      try {
        await this.initKeycloakOnce();
      } catch (error) {
        console.error('❌ [AUTH] Failed to initialize Keycloak for login:', error.message);
        throw new Error('Keycloak initialization failed');
      }
    }

    if (!this.keycloak) {
      console.error('❌ [AUTH] Keycloak not initialized');
      throw new Error('Keycloak not initialized');
    }

    logger.auth('Redirecting to login', { 
      realm: this.config.realm,
      component: 'auth' 
    });
    return this.keycloak.login();
  }

  /**
   * 🚪 Logout user
   */
  async logout() {
    if (!this.keycloak) {
      // 🔐 FIXED: Use console.error instead of logger.error to avoid auth loops
      console.error('❌ [AUTH] Keycloak not initialized');
      throw new Error('Keycloak not initialized');
    }

    // 🔧 IMPORTANT: Save reference to keycloak instance and token BEFORE any async operations
    const keycloakRef = this.keycloak;
    const idToken = keycloakRef?.idToken;

    // � OPTIMIZATION: Build logout URL FIRST, before any cleanup
    const logoutUrl = `${this.config.url}/realms/${this.config.realm}/protocol/openid-connect/logout`;
    const postLogoutRedirectUri = encodeURIComponent(window.location.origin + '/');
    const idTokenHint = idToken ? `&id_token_hint=${idToken}` : '';
    const fullLogoutUrl = `${logoutUrl}?post_logout_redirect_uri=${postLogoutRedirectUri}&client_id=${this.config.clientId}${idTokenHint}`;

    // � IMMEDIATE REDIRECT - no delays, no rerenders!
    // Storage cleanup will happen on next page load
    window.location.href = fullLogoutUrl;
    
    // ⚠️ CODE BELOW NEVER EXECUTES (kept for reference)
    // The redirect above is synchronous and immediate
    
    logger.auth('Redirecting to logout URL', { 
      logoutUrl: fullLogoutUrl,
      component: 'auth' 
    });

    // 🔐 FIXED: Set logger as unauthenticated immediately to prevent 401 loops
    logger.setAuthenticated(false);

    // Stop token refresh immediately
    if (this._refreshInterval) {
      clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }

    // 🔧 FIXED: Reset singleton states to allow fresh initialization after logout
    initPromise = null;
    keycloakInstance = null;
    isInitialized = false;
    this.initialized = false;

    // Clear ALL possible storage locations
    try {
      // Clear localStorage
      Object.keys(localStorage).forEach(key => {
        if (key.includes('keycloak') || key.includes('kc-') || key.includes('auth')) {
          localStorage.removeItem(key);
        }
      });
      
      // Clear sessionStorage
      Object.keys(sessionStorage).forEach(key => {
        if (key.includes('keycloak') || key.includes('kc-') || key.includes('auth')) {
          sessionStorage.removeItem(key);
        }
      });
      
      // 🔧 Už nepotřebujeme logout flags - login-required se postará o auto-login
      
    } catch (error) {
      // 🔐 FIXED: Use console.warn instead of logger.warn to avoid auth issues
      console.warn('⚠️ [AUTH] Error clearing storage:', error.message);
    }

    try {
      // Clear Keycloak instance tokens using saved reference
      if (keycloakRef) {
        keycloakRef.token = null;
        keycloakRef.refreshToken = null;
        keycloakRef.idToken = null;
        keycloakRef.authenticated = false;
      }
      
      // 🔧 FIXED: Clear the Keycloak instance reference completely
      this.keycloak = null;
      
    } catch (error) {
      // 🔐 FIXED: Use console.error instead of logger.error to avoid auth loops
      console.error('❌ [AUTH] Logout error:', error.message);
    }
  }

  /**
   * ✅ Check if user is authenticated
   */
  isAuthenticated() {
    return this.keycloak?.authenticated || false;
  }

  /**
   * 👤 Get user information
   */
  getUserInfo() {
    if (!this.keycloak?.tokenParsed) {
      return null;
    }

    return {
      username: this.keycloak.tokenParsed.preferred_username,
      email: this.keycloak.tokenParsed.email,
      firstName: this.keycloak.tokenParsed.given_name,
      lastName: this.keycloak.tokenParsed.family_name,
      roles: this.keycloak.tokenParsed.realm_access?.roles || [],
      tenant: this.getTenant(),

      // Debug info
      _debug: {
        realm: this.keycloak.realm,
        clientId: this.keycloak.clientId,
        token: this.keycloak.token?.substring(0, 50) + '...'
      }
    };
  }

  /**
   * 🎫 Get access token for API calls
   */
  getToken() {
    return this.keycloak?.token;
  }

  /**
   * 🔍 Get debug information
   */
  getDebugInfo() {
    return {
      config: this.config,
      initialized: this.initialized,
      authenticated: this.isAuthenticated(),
      userInfo: this.getUserInfo(),
      keycloak: this.keycloak ? {
        realm: this.keycloak.realm,
        clientId: this.keycloak.clientId,
        flow: this.keycloak.flow,
        responseMode: this.keycloak.responseMode
      } : null
    };
  }

  /**
   * 🚪 Get logout URL (synchronous - for immediate redirect)
   * This allows AuthProvider to do synchronous redirect without any delays
   */
  getLogoutUrl() {
    if (!this.keycloak) {
      console.warn('⚠️ Keycloak not initialized, cannot build logout URL');
      return '/';
    }

    const idToken = this.keycloak.idToken;
    const logoutUrl = `${this.config.url}/realms/${this.config.realm}/protocol/openid-connect/logout`;
    const postLogoutRedirectUri = encodeURIComponent(window.location.origin + '/');
    const idTokenHint = idToken ? `&id_token_hint=${idToken}` : '';
    
    return `${logoutUrl}?post_logout_redirect_uri=${postLogoutRedirectUri}&client_id=${this.config.clientId}${idTokenHint}`;
  }
}

// Singleton instance
const keycloakService = new KeycloakService();
export default keycloakService;