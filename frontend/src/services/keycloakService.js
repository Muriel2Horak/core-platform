// Keycloak service with auto-detection from hostname
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

    // Use realm from hostname as initial hint
    const realm = hostname.split('.')[0];
    console.log(`🔧 Using initial realm hint from hostname '${hostname}': ${realm}`);

    // 🔧 SSL: Use HTTPS URL via Nginx reverse proxy
    let keycloakUrl;

    if (hostname.includes('localhost')) {
      // In development use localhost with HTTPS
      keycloakUrl = `https://core-platform.local`;
    } else {
      // In production use HTTPS with detected hostname
      keycloakUrl = `https://${hostname}`;
    }

    console.log(`🔧 Using Keycloak HTTPS URL via Nginx proxy: ${keycloakUrl}`);

    return {
      url: keycloakUrl,
      realm: realm,
      clientId: 'web',

      // Debug info
      _debug: {
        hostname,
        protocol,
        initialRealmHint: realm,
        keycloakUrl,
        usingSSL: true,
        usingNginxProxy: true
      }
    };
  }

  /**
   * 🚀 Initialize Keycloak with auto-detected configuration
   */
  async init() {
    if (this.initialized) {
      return this.keycloak;
    }

    try {
      console.log('🔧 Initializing Keycloak with config:', this.config);

      // Import Keycloak dynamically (ES modules compatibility)
      const Keycloak = (await import('keycloak-js')).default;

      this.keycloak = new Keycloak(this.config);

      // Initialization options
      const initOptions = {
        onLoad: 'check-sso',
        pkceMethod: 'S256',

        // Disable all iframe mechanisms to prevent sandbox warnings
        checkLoginIframe: false,
        checkLoginIframeInterval: 0,
        silentCheckSsoFallback: false,

        // Modern token-based authentication settings
        enableLogging: false,
        messageReceiveTimeout: 10000,
        flow: 'standard',
        responseMode: 'fragment',
        silentCheckSsoRedirectUri: undefined,
      };

      const authenticated = await this.keycloak.init(initOptions);

      if (authenticated) {
        console.log('✅ Keycloak authenticated successfully');
        console.log('🎯 Token info:', {
          realm: this.keycloak.realm,
          username: this.keycloak.tokenParsed?.preferred_username,
          tenant: this.getTenant(),
          roles: this.keycloak.tokenParsed?.realm_access?.roles
        });

        // Store token for API calls
        localStorage.setItem('keycloak-token', this.keycloak.token);

        // Setup token refresh
        this.setupTokenRefresh();
      } else {
        console.log('ℹ️ User not authenticated, ready for login');
      }

      this.initialized = true;
      return this.keycloak;

    } catch (error) {
      console.error('❌ Keycloak initialization failed:', error);
      throw error;
    }
  }

  /**
   * 🎯 Get tenant from JWT token issuer
   */
  getTenant() {
    const iss = this.keycloak?.tokenParsed?.iss;
    
    if (iss) {
      // Extract realm from issuer URL like: https://core-platform.local/auth/realms/test-tenant
      const match = iss.match(/\/realms\/([^\/\?#]+)/);
      if (match) {
        const tenant = match[1];
        console.log(`ℹ️ Tenant '${tenant}' derived from token issuer: ${iss}`);
        return tenant;
      }
    }

    console.warn(`⚠️ Could not derive tenant from issuer. Fallback to 'core-platform'. Issuer: ${iss}`);
    return 'core-platform';
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
          console.log('🔄 Token is expiring, attempting refresh...');

          const refreshed = await this.keycloak.updateToken(60);
          if (refreshed) {
            console.log('✅ Token refreshed successfully');
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
        console.error('❌ Token refresh failed:', error);

        if (this.keycloak.isTokenExpired(0)) {
          console.log('🚪 Token expired, logging out...');
          this.logout();
        }
      }
    }, 30000); // Check every 30 seconds

    this._refreshInterval = refreshInterval;
  }

  /**
   * 🔐 Redirect to Keycloak login
   */
  login() {
    if (!this.keycloak) {
      throw new Error('Keycloak not initialized');
    }

    console.log(`🔐 Redirecting to login for realm: ${this.config.realm}`);
    return this.keycloak.login();
  }

  /**
   * 🚪 Logout user
   */
  logout() {
    if (!this.keycloak) {
      throw new Error('Keycloak not initialized');
    }

    console.log('🚪 Logging out');

    if (this._refreshInterval) {
      clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }

    localStorage.removeItem('keycloak-token');
    return this.keycloak.logout();
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