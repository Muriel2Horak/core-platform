// Keycloak service s auto-detekcí tenant z hostname
class KeycloakService {
  constructor() {
    this.keycloak = null;
    this.initialized = false;
    this.tenantCreationInProgress = false;

    // 🎯 AUTO-DETECT: Tenant z hostname nebo fallback
    this.config = this.getKeycloakConfig();
  }

  /**
   * 🌐 SUBDOMAIN AUTO-DETECTION: Automatická detekce Keycloak konfigurace
   */
  getKeycloakConfig() {
    const hostname = window.location.hostname;
    const protocol = window.location.protocol;

    // Detect tenant from subdomain
    const parts = hostname.split('.');
    let tenantKey = 'core-platform'; // default
    let isMainDomain = false; // 🔧 OPRAVENO: explicitní detekce main domain

    if (parts.length >= 3) {
      // e.g., test-tenant.core-platform.local
      const subdomain = parts[0];
      if (subdomain !== 'core-platform') {
        tenantKey = subdomain;
      }
    }

    // 🔧 OPRAVENO: Main domain je pouze když nemáme žádný specifický tenant
    // core-platform.local je validní tenant domain, ne main domain
    if (hostname === 'localhost' || hostname.endsWith('.example.com')) {
      isMainDomain = true;
    }

    console.log(`🔍 Detected tenant from hostname '${hostname}': ${tenantKey}`);

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
      realm: tenantKey, // 🎯 CLEAN ARCHITECTURE: realm = tenant key
      clientId: 'web',

      // Debug info
      _debug: {
        hostname,
        protocol,
        detectedTenant: tenantKey,
        keycloakUrl,
        isMainDomain, // 🔧 OPRAVENO: používej explicitní isMainDomain
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
      return await this.handleInitializationError(error);
    }
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
    const errorDescription = error.error_description?.toLowerCase() || '';
    
    // 🔍 REALM NOT FOUND: Pokus o automatické vytvoření tenant realm
    if (errorMessage.includes('realm') && (errorMessage.includes('not found') || errorMessage.includes('does not exist')) ||
        errorDescription.includes('realm') && errorDescription.includes('not found')) {
      
      console.log('🏗️ Realm not found, attempting tenant auto-creation...');
      return await this.handleMissingRealm();
    }

    // 🔍 NETWORK ERROR: Možná je Keycloak nedostupný
    if (errorMessage.includes('network') || errorMessage.includes('fetch') || error.name === 'TypeError') {
      console.log('🌐 Network error detected, showing connection error...');
      this.showConnectionError();
      throw error;
    }

    // 🔍 MAIN DOMAIN: Redirect na tenant discovery
    if (this.config._debug.isMainDomain) {
      console.log('🔍 Main domain detected - redirecting to tenant discovery');
      window.location.href = '/tenant-discovery';
      return;
    }

    // 🚨 OTHER ERROR: Fallback na main domain
    console.log('🚨 Unknown error, falling back to main domain...');
    this.fallbackToMainDomain(error);
    throw error;
  }

  /**
   * 🏗️ MISSING REALM: Zpracování chybějícího realm
   */
  async handleMissingRealm() {
    if (this.tenantCreationInProgress) {
      console.log('⏳ Tenant creation already in progress...');
      return;
    }

    const tenantKey = this.config._debug.detectedTenant;
    
    // Pokud jsme na main domain, přesměruj na tenant discovery
    if (this.config._debug.isMainDomain) {
      window.location.href = '/tenant-discovery';
      return;
    }

    try {
      this.tenantCreationInProgress = true;
      
      // 🔄 SHOW LOADING: Zobraz loading obrazovku
      this.showTenantCreationDialog(tenantKey);
      
      // 🏗️ CREATE TENANT: Pokus o vytvoření tenant přes API
      const success = await this.attemptTenantCreation(tenantKey);
      
      if (success) {
        // ✅ SUCCESS: Reload page po úspěšném vytvoření
        console.log('✅ Tenant created successfully, reloading page...');
        setTimeout(() => {
          window.location.reload();
        }, 2000);
      } else {
        // ❌ FAILED: Fallback na main domain
        this.showTenantCreationError(tenantKey);
        setTimeout(() => {
          this.fallbackToMainDomain(new Error('Tenant creation failed'));
        }, 3000);
      }
      
    } catch (error) {
      console.error('❌ Tenant creation error:', error);
      this.showTenantCreationError(tenantKey);
      setTimeout(() => {
        this.fallbackToMainDomain(error);
      }, 3000);
    } finally {
      this.tenantCreationInProgress = false;
    }
  }

  /**
   * 🏗️ ATTEMPT TENANT CREATION: Pokus o vytvoření tenantu přes API
   */
  async attemptTenantCreation(tenantKey) {
    try {
      console.log(`🏗️ Attempting to create tenant: ${tenantKey}`);
      
      // API endpoint pro vytvoření tenantu
      const response = await fetch('/api/admin/tenants', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // Pozor: Pro admin operace bychom měli mít autentizaci
          // V produkci by zde měl být Authorization header
        },
        body: JSON.stringify({
          key: tenantKey,
          displayName: this.generateDisplayName(tenantKey),
          autoCreate: true
        })
      });

      if (response.ok) {
        const result = await response.json();
        console.log('✅ Tenant created successfully:', result);
        return true;
      } else {
        const error = await response.json();
        console.error('❌ Tenant creation failed:', error);
        return false;
      }

    } catch (error) {
      console.error('❌ Tenant creation network error:', error);
      return false;
    }
  }

  /**
   * 🏷️ GENERATE DISPLAY NAME: Vytvoří display name z tenant key
   */
  generateDisplayName(tenantKey) {
    // Převeď tenant-key na "Tenant Key"
    return tenantKey
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  /**
   * 💬 UI DIALOGS: Zobrazení dialógů pro tenant creation
   */
  showTenantCreationDialog(tenantKey) {
    const dialog = document.createElement('div');
    dialog.id = 'tenant-creation-dialog';
    dialog.innerHTML = `
      <div style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
                  background: rgba(0,0,0,0.8); display: flex; align-items: center; 
                  justify-content: center; z-index: 10000; font-family: Arial, sans-serif;">
        <div style="background: white; padding: 30px; border-radius: 10px; text-align: center; max-width: 400px;">
          <div style="font-size: 24px; margin-bottom: 10px;">🏗️</div>
          <h3 style="margin: 0 0 15px 0; color: #333;">Creating Tenant</h3>
          <p style="margin: 0 0 20px 0; color: #666;">
            Setting up workspace for <strong>${tenantKey}</strong>...<br>
            This may take a few moments.
          </p>
          <div style="width: 100%; height: 4px; background: #f0f0f0; border-radius: 2px;">
            <div style="width: 0%; height: 100%; background: #007bff; border-radius: 2px; 
                        animation: progress 2s infinite;"></div>
          </div>
        </div>
      </div>
      <style>
        @keyframes progress {
          0% { width: 0%; }
          50% { width: 70%; }
          100% { width: 100%; }
        }
      </style>
    `;
    document.body.appendChild(dialog);
  }

  showTenantCreationError(tenantKey) {
    const dialog = document.getElementById('tenant-creation-dialog');
    if (dialog) {
      dialog.innerHTML = `
        <div style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
                    background: rgba(0,0,0,0.8); display: flex; align-items: center; 
                    justify-content: center; z-index: 10000; font-family: Arial, sans-serif;">
          <div style="background: white; padding: 30px; border-radius: 10px; text-align: center; max-width: 400px;">
            <div style="font-size: 24px; margin-bottom: 10px;">❌</div>
            <h3 style="margin: 0 0 15px 0; color: #d32f2f;">Creation Failed</h3>
            <p style="margin: 0 0 20px 0; color: #666;">
              Unable to create tenant <strong>${tenantKey}</strong>.<br>
              Redirecting to main portal...
            </p>
          </div>
        </div>
      `;
    }
  }

  showConnectionError() {
    const dialog = document.createElement('div');
    dialog.innerHTML = `
      <div style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
                  background: rgba(0,0,0,0.8); display: flex; align-items: center; 
                  justify-content: center; z-index: 10000; font-family: Arial, sans-serif;">
        <div style="background: white; padding: 30px; border-radius: 10px; text-align: center; max-width: 400px;">
          <div style="font-size: 24px; margin-bottom: 10px;">🌐</div>
          <h3 style="margin: 0 0 15px 0; color: #d32f2f;">Connection Error</h3>
          <p style="margin: 0 0 20px 0; color: #666;">
            Unable to connect to authentication service.<br>
            Please check your connection and try again.
          </p>
          <button onclick="window.location.reload()" 
                  style="background: #007bff; color: white; border: none; 
                         padding: 10px 20px; border-radius: 5px; cursor: pointer;">
            Retry
          </button>
        </div>
      </div>
    `;
    document.body.appendChild(dialog);
  }

  /**
   * 🔄 FALLBACK: Přesměrování na main domain při chybě
   */
  fallbackToMainDomain(error) {
    const mainDomain = window.location.hostname.split('.').slice(-2).join('.');
    const fallbackUrl = `https://${mainDomain}/tenant-error?reason=${encodeURIComponent(error.message)}&original=${encodeURIComponent(window.location.hostname)}`;
    
    console.log('🔄 Falling back to main domain:', fallbackUrl);
    window.location.href = fallbackUrl;
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
            
            // Notifikuj o úspěšném refresh
            window.dispatchEvent(new CustomEvent('keycloak-token-refreshed', {
              detail: { token: this.keycloak.token }
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
      tenant: this.keycloak.tokenParsed.tenant,

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
          tenant: userInfo.tenant || this.keycloak.tokenParsed?.tenant,
          
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