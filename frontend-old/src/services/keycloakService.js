import { realmFromIss } from '../utils/tenantUtils';

// Keycloak service s auto-detekcí tenant z hostname
class KeycloakService {
  constructor() {
    this.keycloak = null;
    this.initialized = false;
    this.tenantCreationInProgress = false;

    // 🎯 Odstraněna stará auto-detekce tenanta z hostname
    // Nově se tenant primárně zjišťuje z JWT tokenu po přihlášení.
    this.config = this.getKeycloakConfig();
  }

  /**
   * 🌐 SUBDOMAIN AUTO-DETECTION: Automatická detekce Keycloak konfigurace
   */
  getKeycloakConfig() {
    const hostname = window.location.hostname;
    const protocol = window.location.protocol;

    // Tenant se již nezjišťuje z hostname, ale slouží jen jako fallback pro realm.
    const realm = hostname.split('.')[0];
    console.log(`🔧 Using initial realm hint from hostname '${hostname}': ${realm}`);

    // 🔧 SSL: Používej HTTPS URL přes Nginx reverse proxy s SSL
    let keycloakUrl;

    if (hostname.includes('localhost')) {
      // V development módu používej localhost s HTTPS
      keycloakUrl = `https://core-platform.local`;
    } else {
      // V produkci používej HTTPS s detekovaným hostname
      keycloakUrl = `https://${hostname}`;
    }

    console.log(`🔧 Using Keycloak HTTPS URL via Nginx proxy: ${keycloakUrl}`);

    return {
      url: keycloakUrl,
      realm: realm, // Použijeme realm z hostname jako výchozí bod
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
   * 🚀 INITIALIZE: Inicializuje Keycloak s auto-detekovanou konfigurací
   */
  async init() {
    if (this.initialized) {
      return this.keycloak;
    }

    try {
      console.log('🔧 Initializing Keycloak with config:', this.config);

      // Import Keycloak dynamicky (Vite compatibility)
      const Keycloak = (await import('keycloak-js')).default;

      this.keycloak = new Keycloak(this.config);

      // Initialization options
      const initOptions = {
        onLoad: 'check-sso', // 🔧 FIX: Změna z 'login-required' na 'check-sso' pro lepší UX
        pkceMethod: 'S256',

        // 🔧 FIX: KOMPLETNÍ VYPNUTÍ všech iframe mechanismů pro odstranění sandbox warnings
        checkLoginIframe: false, // Vypneme login-status-iframe.html
        checkLoginIframeInterval: 0, // Vypneme interval checking
        silentCheckSsoFallback: false, // Vypneme 3p-cookies detekci (step1/step2.html)

        // 🔧 FIX: Vypneme i silent SSO iframe (způsobuje další sandbox warning)
        // silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html', // VYPNUTO

        // 🔧 FIX: Modernější nastavení pro čistě token-based autentizaci
        enableLogging: false, // Disable Keycloak logging to reduce console noise
        messageReceiveTimeout: 10000, // Timeout pro zprávy (i když nepoužíváme iframe)

        // 🔧 FIX: Token-only session tracking - bez jakýchkoli iframe
        flow: 'standard', // Standard Authorization Code Flow
        responseMode: 'fragment', // Fragment mode pro lepší bezpečnost

        // 🔧 FIX: Explicitně zakážeme všechny iframe mechanismy
        silentCheckSsoRedirectUri: undefined, // Úplně vypneme silent SSO iframe
      };

      const authenticated = await this.keycloak.init(initOptions);

      if (authenticated) {
        console.log('✅ Keycloak authenticated successfully');
        console.log('🎯 Token info:', {
          realm: this.keycloak.realm,
          username: this.keycloak.tokenParsed?.preferred_username,
          tenant: this.keycloak.tokenParsed?.tenant,
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
      // Zde již nevoláme handleInitializationError, protože se přesouvá do App.jsx
      throw error; // Vyhodíme chybu, aby ji mohl zpracovat volající
    }
  }

  /**
   * 🎯 GET TENANT: Získá tenanta z issueru v JWT tokenu.
   * @returns {string} Název tenanta.
   */
  getTenant() {
    const iss = this.keycloak?.tokenParsed?.iss;
    const tenant = realmFromIss(iss);

    if (tenant) {
      console.log(`ℹ️ Tenant '${tenant}' derived from token issuer: ${iss}`);
      return tenant;
    }

    console.warn(`⚠️ Could not derive tenant from issuer. Fallback to 'core-platform'. Issuer: ${iss}`);
    return 'core-platform';
  }

  /**
   * 🔄 INITIALIZE ALIAS: Zpětná kompatibilita s původní implementací
   * Tato metoda zajišťuje kompatibilitu s App.jsx a AuthService
   */
  async initialize() {
    const keycloak = await this.init();

    if (keycloak && keycloak.authenticated) {
      // 🔧 DŮLEŽITÉ: Zajisti kompatibilitu s AuthService
      // AuthService očekává user info v localStorage
      const userInfo = this.getUserInfo();
      if (userInfo) {
        // Ulož user info pro AuthService
        localStorage.setItem('keycloak-user-info', JSON.stringify(userInfo));

        console.log('✅ User info stored for AuthService:', {
          username: userInfo.username,
          tenant: userInfo.tenant,
          roles: userInfo.roles
        });
      }

      // 🔧 Notify AuthService o úspěšné autentizaci
      window.dispatchEvent(new CustomEvent('keycloak-authenticated', {
        detail: { userInfo, token: this.keycloak.token }
      }));
    }

    return keycloak;
  }

  /**
   * 🚨 ERROR HANDLING: Zpracování chyb při inicializaci
   */
  async handleInitializationError(error) {
    const errorMessage = error.message?.toLowerCase() || '';
    const _errorDescription = error.error_description?.toLowerCase() || '';

    // 🔍 NETWORK ERROR: Možná je Keycloak nedostupný
    if (errorMessage.includes('network') || errorMessage.includes('fetch') || error.name === 'TypeError') {
      console.log('🌐 Network error detected, showing connection error...');
      this.showConnectionError();
      throw error;
    }

    // 🚨 OTHER ERROR: Fallback na main domain
    console.log('🚨 Unknown error, falling back to main domain...');
    this.fallbackToMainDomain(error);
    throw error;
  }

  /**
   * 🔄 TOKEN REFRESH: Automatické obnovování tokenu
   */
  setupTokenRefresh() {
    if (!this.keycloak) return;

    // 🔧 FIX: Modernější token refresh bez iframe dependency
    // Používáme pouze token-based refresh místo iframe session monitoring
    const refreshInterval = setInterval(async () => {
      try {
        // Kontrola pouze pokud je uživatel stále přihlášen
        if (!this.keycloak.authenticated) {
          clearInterval(refreshInterval);
          return;
        }

        // Refresh token když je blízko expirace (60s před expirací)
        if (this.keycloak.isTokenExpired(60)) {
          console.log('🔄 Token is expiring, attempting refresh...');

          const refreshed = await this.keycloak.updateToken(60);
          if (refreshed) {
            console.log('✅ Token refreshed successfully');
            localStorage.setItem('keycloak-token', this.keycloak.token);

            // Notifikuj o úspěšném refresh, aby se mohl aktualizovat tenant
            window.dispatchEvent(new CustomEvent('keycloak-token-refreshed', {
              detail: { 
                token: this.keycloak.token,
                tenant: this.getTenant() // Přidáme nově odvozeného tenanta
              }
            }));
          } else {
            console.log('ℹ️ Token still valid, no refresh needed');
          }
        }
      } catch (error) {
        console.error('❌ Token refresh failed:', error);

        // Při chybě refresh se pokus o logout pouze pokud je token skutečně expirovaný
        if (this.keycloak.isTokenExpired(0)) {
          console.log('🚪 Token expired, logging out...');
          this.logout();
        }
      }
    }, 30000); // Kontroluj každých 30 sekund (místo 10s pro lepší performance)

    // Uložíme interval reference pro možné čištění
    this._refreshInterval = refreshInterval;
  }

  /**
   * 🔐 LOGIN: Přesměruje na Keycloak login
   */
  login() {
    if (!this.keycloak) {
      throw new Error('Keycloak not initialized');
    }

    console.log(`🔐 Redirecting to login for realm: ${this.config.realm}`);
    return this.keycloak.login();
  }

  /**
   * 🚪 LOGOUT: Odhlásí uživatele
   */
  logout() {
    if (!this.keycloak) {
      throw new Error('Keycloak not initialized');
    }

    console.log('🚪 Logging out');

    // 🔧 FIX: Vyčistíme token refresh interval
    if (this._refreshInterval) {
      clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }

    localStorage.removeItem('keycloak-token');
    localStorage.removeItem('keycloak-user-info');

    return this.keycloak.logout();
  }

  /**
   * ✅ AUTH CHECK: Kontrola, zda je uživatel přihlášen
   */
  isAuthenticated() {
    return this.keycloak?.authenticated || false;
  }

  /**
   * 👤 USER INFO: Získání informací o uživateli
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
      tenant: this.getTenant(), // Použijeme novou metodu pro získání tenanta

      // Debug info
      _debug: {
        realm: this.keycloak.realm,
        clientId: this.keycloak.clientId,
        token: this.keycloak.token?.substring(0, 50) + '...'
      }
    };
  }

  /**
   * 🔄 GET FRESH USER INFO: Získání aktuálních informací o uživateli z Keycloak userinfo endpointu
   */
  async getUserInfoFresh() {
    if (!this.keycloak || !this.isAuthenticated()) {
      throw new Error('User not authenticated');
    }

    try {
      // Nejdříve zkus načíst z Keycloak userinfo endpointu
      const response = await fetch(`${this.config.url}/realms/${this.config.realm}/protocol/openid-connect/userinfo`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const userInfo = await response.json();

        return {
          username: userInfo.preferred_username,
          email: userInfo.email,
          firstName: userInfo.given_name,
          lastName: userInfo.family_name,
          roles: this.keycloak.tokenParsed?.realm_access?.roles || [],
          tenant: this.getTenant(), // Použijeme novou metodu

          // Rozšířené informace z userinfo endpointu
          sub: userInfo.sub,
          name: userInfo.name,
          emailVerified: userInfo.email_verified,

          // Debug info
          _debug: {
            realm: this.keycloak.realm,
            source: 'userinfo-endpoint'
          }
        };
      } else {
        console.warn('Failed to fetch fresh user info, falling back to token info');
        return this.getUserInfo();
      }
    } catch (error) {
      console.error('Error fetching fresh user info:', error);
      // Fallback na základní getUserInfo z tokenu
      return this.getUserInfo();
    }
  }

  /**
   * 🎫 GET TOKEN: Získání access tokenu pro API volání
   */
  getToken() {
    return this.keycloak?.token;
  }

  /**
   * 🔧 ACCOUNT CONSOLE: Otevře Keycloak Account Console
   */
  openAccountConsole() {
    if (!this.keycloak) {
      throw new Error('Keycloak not initialized');
    }

    const accountUrl = `${this.config.url}/realms/${this.config.realm}/account`;
    window.open(accountUrl, '_blank');
  }

  /**
   * 🔍 DEBUG INFO: Diagnostické informace
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