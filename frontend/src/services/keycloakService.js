import Keycloak from 'keycloak-js';

// 🔑 Keycloak instance - singleton
let keycloakInstance = null;
let initializationPromise = null;

/**
 * ✅ OPRAVENO: Keycloak 26.x integrace s environment proměnnými
 * - Používá VITE_KEYCLOAK_* proměnné z docker-compose.yml
 * - Keycloak běží na root path (/) bez /auth prefixu
 * - Nginx reverse proxy zajišťuje routing z /auth/* na Keycloak root
 * - Žádné hardcoded domény nebo URL
 */
class KeycloakService {
  constructor() {
    this.keycloak = null;
    this.token = null;
    this.refreshToken = null;
    this.isInitialized = false;
  }

  /**
   * Inicializace Keycloak - volá se při startu aplikace
   */
  async initialize() {
    if (initializationPromise) {
      return initializationPromise;
    }

    initializationPromise = this._doInitialize();
    return initializationPromise;
  }

  async _doInitialize() {
    try {
      console.log('🔑 Keycloak: Inicializuji s environment proměnnými...');
      
      // ✅ OPRAVENO: Načítá z environment proměnných místo keycloak.json
      const keycloakConfig = {
        url: import.meta.env.VITE_KEYCLOAK_URL || window.location.origin,
        realm: import.meta.env.VITE_KEYCLOAK_REALM || 'core-platform',
        clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'web'
      };
      
      console.log('🔑 Keycloak: Konfigurace z environment:', keycloakConfig);

      // Vytvoř Keycloak instanci
      this.keycloak = new Keycloak(keycloakConfig);
      keycloakInstance = this.keycloak;

      // ✅ OPRAVENO: Keycloak 26.x optimalizovaná inicializace bez silent-sso.html
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,  // vypnout iframe session check pokud dělá problémy
        pkceMethod: 'S256',       // moderní PKCE flow pro Keycloak 26.x
      });

      if (authenticated) {
        console.log('🔑 Keycloak: Uživatel přihlášen');
        
        // Ulož tokeny v paměti
        this.token = this.keycloak.token;
        this.refreshToken = this.keycloak.refreshToken;
        
        // 🎯 NOVÁ FUNKCE: Ulož token do localStorage pro AuthService
        if (this.keycloak.token) {
          localStorage.setItem('keycloak-token', this.keycloak.token);
          console.log('🔑 Keycloak: Token uložen do localStorage pro AuthService');
        }
        if (this.keycloak.refreshToken) {
          localStorage.setItem('keycloak-refresh-token', this.keycloak.refreshToken);
        }
        
        this.isInitialized = true;

        // Nastav automatické obnovování tokenů
        this.setupTokenRefresh();
        
        return true;
      } else {
        console.log('🔑 Keycloak: Uživatel není přihlášen');
        this.isInitialized = true;
        return false;
      }
    } catch (error) {
      console.error('🔑 Keycloak: Chyba při inicializaci:', error);
      throw error;
    }
  }

  /**
   * Nastav automatické obnovování tokenů
   */
  setupTokenRefresh() {
    if (!this.keycloak) return;

    // Obnov token pokud vyprší za méně než 30 sekund
    setInterval(() => {
      this.keycloak.updateToken(30).then((refreshed) => {
        if (refreshed) {
          console.log('🔑 Keycloak: Token obnoven');
          this.token = this.keycloak.token;
          this.refreshToken = this.keycloak.refreshToken;
          
          // 🎯 NOVÁ FUNKCE: Synchronizuj obnovený token s localStorage
          if (this.keycloak.token) {
            localStorage.setItem('keycloak-token', this.keycloak.token);
            console.log('🔑 Keycloak: Obnovený token synchronizován s localStorage');
          }
        }
      }).catch((error) => {
        console.error('🔑 Keycloak: Chyba při obnovování tokenu:', error);
        this.logout();
      });
    }, 10000); // Kontroluj každých 10 sekund
  }

  /**
   * Získej aktuální access token
   */
  getToken() {
    return this.token;
  }

  /**
   * Zkontroluj, zda je uživatel přihlášen
   */
  isAuthenticated() {
    return this.isInitialized && this.keycloak?.authenticated;
  }

  /**
   * Získej informace o uživateli
   */
  getUserInfo() {
    if (!this.keycloak?.tokenParsed) return null;

    const token = this.keycloak.tokenParsed;
    return {
      username: token.preferred_username,
      email: token.email,
      firstName: token.given_name,
      lastName: token.family_name,
      roles: token.realm_access?.roles || []
    };
  }

  /**
   * Získej aktuální informace o uživateli z Keycloak serveru (fresh data)
   */
  async getUserInfoFresh() {
    if (!this.keycloak || !this.isAuthenticated()) {
      throw new Error('Keycloak not initialized or user not authenticated');
    }

    try {
      console.log('🔑 Keycloak: Načítám fresh user info ze serveru...');
      
      // Získej aktuální data z Keycloak serveru
      const userInfo = await this.keycloak.loadUserInfo();
      console.log('🔑 Keycloak: Fresh user info načtena:', userInfo);
      
      // Kombinuj s daty z tokenu
      const tokenData = this.keycloak.tokenParsed;
      
      const freshUserData = {
        // Základní údaje
        username: userInfo.preferred_username || tokenData?.preferred_username,
        email: userInfo.email || tokenData?.email,
        firstName: userInfo.given_name || tokenData?.given_name,
        lastName: userInfo.family_name || tokenData?.family_name,
        name: userInfo.name || tokenData?.name,
        
        // Role z tokenu (nejčerstvější)
        roles: tokenData?.realm_access?.roles || [],
        
        // Rozšířené atributy z Keycloak (custom fields)
        department: userInfo.department,
        manager: userInfo.manager,
        position: userInfo.position,
        phone: userInfo.phone_number,
        locale: userInfo.locale,
        
        // Metadata
        emailVerified: userInfo.email_verified,
        updatedAt: userInfo.updated_at,
        
        // Token info
        tokenIssuedAt: new Date(tokenData?.iat * 1000),
        tokenExpiresAt: new Date(tokenData?.exp * 1000),
        issuer: tokenData?.iss
      };
      
      console.log('🔑 Keycloak: Zpracovaná fresh data:', freshUserData);
      return freshUserData;
      
    } catch (error) {
      console.error('🔑 Keycloak: Chyba při načítání fresh user info:', error);
      
      // Fallback na data z tokenu pokud server selže
      const tokenData = this.keycloak.tokenParsed;
      if (tokenData) {
        console.log('🔑 Keycloak: Používám fallback data z tokenu');
        return {
          username: tokenData.preferred_username,
          email: tokenData.email,
          firstName: tokenData.given_name,
          lastName: tokenData.family_name,
          name: tokenData.name,
          roles: tokenData.realm_access?.roles || [],
          tokenIssuedAt: new Date(tokenData.iat * 1000),
          tokenExpiresAt: new Date(tokenData.exp * 1000),
          issuer: tokenData.iss
        };
      }
      
      throw error;
    }
  }

  /**
   * Odhlásit uživatele
   */
  logout() {
    // 🎯 NOVÁ FUNKCE: Vyčisti localStorage při odhlášení
    localStorage.removeItem('keycloak-token');
    localStorage.removeItem('keycloak-refresh-token');
    localStorage.removeItem('keycloak-id-token');
    console.log('🔑 Keycloak: localStorage vyčištěn při logout');
    
    if (this.keycloak) {
      this.keycloak.logout({
        redirectUri: window.location.origin
      });
    }
  }

  /**
   * Přesměruj na Keycloak Account Console pro změnu hesla/údajů
   */
  openAccountConsole() {
    if (this.keycloak) {
      const accountUrl = `${this.keycloak.authServerUrl}/realms/${this.keycloak.realm}/account`;
      window.open(accountUrl, '_blank');
    }
  }

  /**
   * Přesměruj přímo na změnu hesla v Keycloak Account Console
   */
  openPasswordChange() {
    if (this.keycloak) {
      const passwordUrl = `${this.keycloak.authServerUrl}/realms/${this.keycloak.realm}/account/password`;
      window.open(passwordUrl, '_blank');
    }
  }

  /**
   * Přesměruj na osobní údaje v Keycloak Account Console
   */
  openPersonalInfo() {
    if (this.keycloak) {
      const personalUrl = `${this.keycloak.authServerUrl}/realms/${this.keycloak.realm}/account/personal-info`;
      window.open(personalUrl, '_blank');
    }
  }

  /**
   * Získej URL pro Keycloak Account Console
   */
  getAccountConsoleUrl() {
    if (this.keycloak) {
      return `${this.keycloak.authServerUrl}/realms/${this.keycloak.realm}/account`;
    }
    return null;
  }

  /**
   * HTTP interceptor - přidá Authorization header ke všem API požadavkům
   */
  async apiCall(url, options = {}) {
    const token = this.getToken();
    
    if (!token) {
      throw new Error('No access token available');
    }

    // Přidej Authorization header
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers
    };

    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include'
    });

    // Pokud 401, zkus obnovit token a opakuj požadavek
    if (response.status === 401 && this.keycloak) {
      try {
        const refreshed = await this.keycloak.updateToken(5);
        if (refreshed) {
          this.token = this.keycloak.token;
          // Opakuj požadavek s novým tokenem
          headers['Authorization'] = `Bearer ${this.token}`;
          return fetch(url, { ...options, headers, credentials: 'include' });
        }
      } catch (error) {
        console.error('🔑 Keycloak: Nelze obnovit token:', error);
        this.logout();
        throw error;
      }
    }

    return response;
  }

  /**
   * Manuální přihlášení - zavolá se když user klikne na login tlačítko
   */
  login() {
    if (this.keycloak) {
      this.keycloak.login({
        redirectUri: window.location.origin
      });
    }
  }
}

// Export singleton instance
export const keycloakService = new KeycloakService();
export { keycloakInstance }; // Pro případy kdy potřebujeme přímý přístup
export default keycloakService;