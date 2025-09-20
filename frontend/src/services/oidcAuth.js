// OIDC Authorization Code Flow service
class OIDCAuthService {
  constructor() {
    this.keycloakConfig = {
      url: 'http://localhost/auth',   // 🔑 veřejná URL (za NGINX)
      realm: 'core-platform',
      clientId: 'core-backend',  // Používáme backend client
    };
    
    this.logger = null;
    this._initLogger();
  }

  // Inicializace loggeru
  async _initLogger() {
    try {
      const module = await import('./logger.js');
      this.logger = module.default;
    } catch (error) {
      console.warn('Failed to load logger:', error);
    }
  }

  // Logování s fallback
  _log(message, extra = {}) {
    console.log(`🔑 OIDC: ${message}`, extra);
    if (this.logger) {
      // 🔧 FIX: Používám správnou logger metodu
      this.logger.info('OIDC_FLOW', message, extra);
    }
  }

  /**
   * 🔑 Krok 1: Zahájení OIDC Authorization Code Flow
   */
  async initiateLogin(redirectPath = '/') {
    try {
      this._log('Zahajuji OIDC Authorization Code Flow', { redirectPath });
      
      // Uložíme redirect path pro po-loginové přesměrování
      sessionStorage.setItem('oidc_redirect_path', redirectPath);
      
      // Zavoláme backend pro authorization URL
      const response = await fetch('/api/auth/authorize', {
        method: 'GET',
        credentials: 'include'
      });
      
      if (!response.ok) {
        throw new Error(`Authorization request failed: ${response.status}`);
      }
      
      const authData = await response.json();
      this._log('Získali jsme authorization URL', { 
        hasUrl: !!authData.authorizationUrl,
        hasState: !!authData.state 
      });
      
      // Uložíme PKCE parametry pro callback
      sessionStorage.setItem('oidc_state', authData.state);
      sessionStorage.setItem('oidc_code_verifier', authData.codeVerifier);
      
      // Přesměrujeme na Keycloak login
      window.location.href = authData.authorizationUrl;
      
    } catch (error) {
      this._log('Chyba při zahájení OIDC flow', { error: error.message });
      throw error;
    }
  }

  /**
   * 🔑 Krok 2: Zpracování callback z Keycloak
   */
  async handleCallback() {
    try {
      const urlParams = new URLSearchParams(window.location.search);
      const code = urlParams.get('code');
      const state = urlParams.get('state');
      const error = urlParams.get('error');
      
      if (error) {
        throw new Error(`Authorization failed: ${error}`);
      }
      
      if (!code || !state) {
        throw new Error('Missing authorization code or state parameter');
      }
      
      // Ověříme state parameter
      const storedState = sessionStorage.getItem('oidc_state');
      if (state !== storedState) {
        throw new Error('Invalid state parameter - possible CSRF attack');
      }
      
      this._log('Zpracovávám authorization callback', { hasCode: !!code, stateValid: true });
      
      // Zavoláme backend callback endpoint
      const response = await fetch(`/api/auth/callback?code=${code}&state=${state}`, {
        method: 'GET',
        credentials: 'include'
      });
      
      if (!response.ok) {
        throw new Error(`Callback processing failed: ${response.status}`);
      }
      
      const callbackData = await response.json();
      this._log('Callback úspěšně zpracován', callbackData);
      
      // Vyčistíme session storage
      sessionStorage.removeItem('oidc_state');
      sessionStorage.removeItem('oidc_code_verifier');
      
      // Přesměrujeme na původní stránku
      const redirectPath = sessionStorage.getItem('oidc_redirect_path') || '/';
      sessionStorage.removeItem('oidc_redirect_path');
      
      window.location.href = redirectPath;
      
    } catch (error) {
      this._log('Chyba při zpracování callback', { error: error.message });
      throw error;
    }
  }

  /**
   * 🔑 Kontrola přihlášení
   */
  async isAuthenticated() {
    try {
      // Zkusíme volat chráněný endpoint
      const response = await fetch('/api/auth/userinfo', {
        method: 'GET',
        credentials: 'include'
      });
      
      return response.ok;
    } catch (error) {
      this._log('Chyba při kontrole autentizace', { error: error.message });
      return false;
    }
  }

  /**
   * 🔑 Získání informací o uživateli
   */
  async getUserInfo() {
    try {
      const response = await fetch('/api/auth/userinfo', {
        method: 'GET',
        credentials: 'include'
      });
      
      if (!response.ok) {
        throw new Error(`Failed to get user info: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      this._log('Chyba při získávání user info', { error: error.message });
      return null;
    }
  }

  /**
   * 🔑 API volání s automatickou autentizací
   */
  async apiCall(url, options = {}) {
    try {
      const fullUrl = `/api${url}`;
      
      const response = await fetch(fullUrl, {
        ...options,
        credentials: 'include'  // Session-based auth
      });
      
      if (response.status === 401) {
        this._log('API call returned 401 - redirecting to login');
        this.initiateLogin(window.location.pathname);
        return null;
      }
      
      return response;
      
    } catch (error) {
      this._log('API call failed', { url, error: error.message });
      throw error;
    }
  }

  /**
   * 🔑 Odhlášení
   */
  async logout() {
    try {
      this._log('Zahajuji odhlášení');
      
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      });
      
      // Přesměrujeme na login bez ohledu na response
      window.location.href = '/login';
      
    } catch (error) {
      this._log('Chyba při odhlášení', { error: error.message });
      // Přesměrujeme na login i při chybě
      window.location.href = '/login';
    }
  }
}

// Export singleton instance
export const oidcAuthService = new OIDCAuthService();
export default oidcAuthService;