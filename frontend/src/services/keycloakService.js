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

    // Use realm from hostname - unified logic for all realms
    const realm = hostname.split('.')[0];
    logger.debug('Using realm from hostname', { 
      hostname, 
      realm,
      component: 'auth' 
    });

    // 🔧 Simple: Use the current hostname with HTTPS
    const keycloakUrl = `https://${hostname}`;

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

      // 🔧 Initialize with disabled checkLoginIframe and custom silentCheckSsoRedirectUri
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        checkLoginIframe: false, // 🔧 VYPNUTO podle požadavku
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
      const errorStack = error?.stack || 'No stack trace available';
      
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
      const match = iss.match(/\/realms\/([^\/\?#]+)/);
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

    logger.auth('Logging out', { component: 'auth' });

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
      
      // Set logout flag BEFORE logout attempt
      localStorage.setItem('logout-completed', Date.now().toString());
      localStorage.setItem('prevent-auto-login', 'true');
      
    } catch (error) {
      // 🔐 FIXED: Use console.warn instead of logger.warn to avoid auth issues
      console.warn('⚠️ [AUTH] Error clearing storage:', error.message);
    }

    try {
      // Get current tokens before clearing
      const idToken = this.keycloak.idToken;
      
      // Clear Keycloak instance tokens
      this.keycloak.token = null;
      this.keycloak.refreshToken = null;
      this.keycloak.idToken = null;
      this.keycloak.authenticated = false;
      
      // 🔧 FIXED: Clear the Keycloak instance reference completely
      this.keycloak = null;
      
      // Construct logout URL manually for better control
      const logoutUrl = `${this.config.url}/realms/${this.config.realm}/protocol/openid-connect/logout`;
      const postLogoutRedirectUri = encodeURIComponent(window.location.origin + '/logged-out');
      const idTokenHint = idToken ? `&id_token_hint=${idToken}` : '';
      
      const fullLogoutUrl = `${logoutUrl}?post_logout_redirect_uri=${postLogoutRedirectUri}&client_id=${this.config.clientId}${idTokenHint}`;
      
      logger.auth('Redirecting to logout URL', { 
        logoutUrl: fullLogoutUrl,
        component: 'auth' 
      });
      
      // Force redirect to logout
      window.location.href = fullLogoutUrl;
      
    } catch (error) {
      // 🔐 FIXED: Use console.error instead of logger.error to avoid auth loops
      console.error('❌ [AUTH] Logout error:', error.message);
      // Fallback - just redirect to logged-out page
      window.location.href = '/logged-out';
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
}

// Singleton instance
const keycloakService = new KeycloakService();
export default keycloakService;