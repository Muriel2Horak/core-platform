// Auth utility pro správu JWT tokenů a API volání
class AuthService {
  constructor() {
    // Backend používá cookies, ale pro kompatibilitu kontrolujeme i localStorage
    this.token = localStorage.getItem('keycloak-token');
    this.logger = null;
    this.loggerPromise = null;
    this._initLogger();
    this._log('AUTHSERVICE: Inicializace', { hasToken: !!this.token });
  }

  // Inicializace loggeru při startu
  async _initLogger() {
    if (this.loggerPromise) return this.loggerPromise;
    
    this.loggerPromise = import('./logger.js').then(module => {
      this.logger = module.default;
      return this.logger;
    }).catch(error => {
      console.warn('Failed to load logger:', error);
      return null;
    });
    
    return this.loggerPromise;
  }

  // Spolehlivé logování s fallback
  async _log(message, extra = {}) {
    try {
      if (!this.logger) {
        await this._initLogger();
      }
      
      if (this.logger) {
        this.logger.service(message, extra);
      } else {
        // Fallback na console.log
        console.log(`🔧 ${message}`, extra);
      }
    } catch {
      // Fallback na console.log
      console.log(`🔧 ${message}`, extra);
    }
  }

  async _logError(message, extra = {}) {
    try {
      if (!this.logger) {
        await this._initLogger();
      }
      
      if (this.logger) {
        this.logger.error(message, extra);
      } else {
        console.error(`❌ ${message}`, extra);
      }
    } catch {
      console.error(`❌ ${message}`, extra);
    }
  }

  // Kontrola, zda je uživatel přihlášený - použijeme localStorage token pro Docker kompatibilitu
  async isAuthenticated() {
    this._log('AUTHSERVICE: isAuthenticated() - kontrolujem localStorage token');
    
    // Nejdříve zkontroluj localStorage
    const token = localStorage.getItem('keycloak-token');
    if (!token) {
      this._log('AUTHSERVICE: Žádný token v localStorage');
      return false;
    }

    // Zkontroluj, zda token není vypršený
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Date.now() / 1000;
      
      if (payload.exp && payload.exp < currentTime) {
        this._log('AUTHSERVICE: Token vypršel, odstraňuji z localStorage');
        localStorage.removeItem('keycloak-token');
        return false;
      }
      
      this._log('AUTHSERVICE: Token je platný', { 
        exp: new Date(payload.exp * 1000),
        timeLeft: Math.round((payload.exp - currentTime) / 60) + ' minut'
      });
      
      this.token = token;
      return true;
      
    } catch (error) {
      this._logError('AUTHSERVICE: Chyba při parsování tokenu', error);
      localStorage.removeItem('keycloak-token');
      return false;
    }
  }

  // Synchronní kontrola pro rychlé použití
  isAuthenticatedSync() {
    // Rychlá kontrola - pokud máme token v localStorage, předpokládáme přihlášení
    return !!this.token || this.hasCookies();
  }

  // Kontrola existence auth cookies
  hasCookies() {
    return document.cookie.includes('at=') || document.cookie.includes('rt=');
  }

  // Kontrola vypršení tokenu - pro kompatibilitu
  isTokenExpired() {
    if (!this.token) return true;
    
    try {
      const payload = JSON.parse(atob(this.token.split('.')[1]));
      const currentTime = Date.now() / 1000;
      return payload.exp < currentTime;
    } catch (error) {
      console.error('Error parsing token:', error);
      return true;
    }
  }

  // Získání tokenu pro API volání (bez require, pouze lokální cache/localStorage)
  getToken() {
    return this.token || localStorage.getItem('keycloak-token');
  }

  // Získání informací o uživateli z tokenu
  async getUserInfo() {
    const token = localStorage.getItem('keycloak-token');
    if (!token) return null;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        username: payload.preferred_username || payload.sub,
        email: payload.email,
        name: payload.name,
        firstName: payload.given_name,
        lastName: payload.family_name,
        roles: payload.realm_access?.roles || []
      };
    } catch (error) {
      console.error('🔧 AUTHSERVICE: Chyba při získávání user info:', error);
      return null;
    }
  }

  // Synchronní verze pro rychlé použití (fallback na token parsing)
  getUserInfoSync() {
    if (!this.token) return null;
    
    try {
      const payload = JSON.parse(atob(this.token.split('.')[1]));
      return {
        username: payload.preferred_username || payload.sub,
        email: payload.email,
        name: payload.name,
        roles: payload.realm_access?.roles || []
      };
    } catch (error) {
      console.error('Error parsing user info:', error);
      return null;
    }
  }

  // Přesměrování na přihlašovací stránku
  redirectToLogin() {
    const currentUrl = encodeURIComponent(window.location.origin + window.location.pathname + window.location.search);
    this._log('AUTHSERVICE: Přesměrovávám na Keycloak login relativně', { currentUrl });
    // Relativní přesměrování na Keycloak login endpoint přes NGINX
    const keycloakLoginUrl = `${window.location.origin}/realms/core-platform/protocol/openid-connect/auth?client_id=web&redirect_uri=${currentUrl}&response_type=code&scope=openid%20profile%20email`;
    window.location.href = keycloakLoginUrl;
  }

  // Odhlášení přes backend API a vyčištění localStorage
  async logout() {
    this._log('AUTHSERVICE: Odhlašuji uživatele');
    
    // 🎯 PŘIDÁNO: Získáme user info před odhlášením pro log
    const userInfo = await this.getUserInfo();
    
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      });
      
      // 🎯 PŘIDÁNO: Frontend auth log pro úspěšné odhlášení
      if (this.logger) {
        this.logger.authLogout({
          username: userInfo?.username || 'unknown',
          method: 'manual',
          timestamp: new Date().toISOString()
        });
      } else {
        // Fallback - async load logger
        import('./logger.js').then(module => {
          module.default.authLogout({
            username: userInfo?.username || 'unknown',
            method: 'manual',
            timestamp: new Date().toISOString()
          });
        }).catch(() => {});
      }
      
    } catch (error) {
      this._logError('AUTHSERVICE: Logout error', error);
    } finally {
      // Vyčistit localStorage
      localStorage.removeItem('keycloak-token');
      localStorage.removeItem('keycloak-refresh-token');
      localStorage.removeItem('keycloak-id-token');
      this.token = null;
      this._log('AUTHSERVICE: localStorage vyčištěn, přesměrovávám na login');
      // 🔧 FIX: Přesměrování na SimpleLoginPage místo starého /auth
      window.location.href = '/login';
    }
  }

  // API volání s automatickým přidáním tokenu
  async apiCall(url, options = {}) {
    // Vždy se pokus o osvěžení tokenu přes keycloakService před voláním
    try {
      const ksModule = await import('./keycloakService');
      const ks = ksModule.default;
      if (ks?.keycloak && ks.isAuthenticated()) {
        try {
          await ks.keycloak.updateToken(30);
          // Synchronizuj token do localStorage pro kompatibilitu
          const fresh = ks.getToken();
          if (fresh) localStorage.setItem('keycloak-token', fresh);
        } catch (e) {
          this._logError('AUTHSERVICE: updateToken před voláním selhal', { error: e?.message });
        }
      }
    } catch {
      // keycloakService nemusí být dostupný – použijeme localStorage fallback
    }

    const token = this.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const fullUrl = `/api${url}`; // relativně přes Nginx proxy

    this._log('AUTHSERVICE: API call start', {
      url: url,
      fullUrl: fullUrl,
      method: options.method || 'GET',
      hasToken: !!token,
      hasBody: !!options.body,
      architecture: 'nginx-proxy'
    });

    try {
      let response = await fetch(fullUrl, {
        ...options,
        headers,
        credentials: 'include'
      });

      this._log('AUTHSERVICE: API call response', {
        url: url,
        fullUrl: fullUrl,
        status: response.status,
        ok: response.ok,
        statusText: response.statusText
      });

      if (response.status === 401) {
        // Pokus o refresh tokenu a jednorázový retry
        try {
          const ksModule = await import('./keycloakService');
          const ks = ksModule.default;
          if (ks?.keycloak) {
            const refreshed = await ks.keycloak.updateToken(10);
            if (refreshed) {
              const newToken = ks.getToken();
              if (newToken) localStorage.setItem('keycloak-token', newToken);
              const retryHeaders = { ...headers, Authorization: `Bearer ${newToken}` };
              response = await fetch(fullUrl, { ...options, headers: retryHeaders, credentials: 'include' });
            }
          }
        } catch {
          this._logError('AUTHSERVICE: Refresh při 401 selhal');
        }
      }

      if (response.status === 401) {
        // Token vypršel nebo je neplatný i po pokusu o refresh
        this._logError('AUTHSERVICE: API call returned 401 - unauthorized', {
          url: url,
          fullUrl: fullUrl,
          hasToken: !!token,
          tokenExpired: this.isTokenExpired()
        });

        if (this.logger) {
          this.logger.security('API_401_UNAUTHORIZED', 'API call returned 401 - token invalid or expired', {
            url: url,
            fullUrl: fullUrl,
            method: options.method || 'GET',
            hasToken: !!token,
            timestamp: new Date().toISOString()
          });
        }

        this.logout();
        return null;
      }

      return response;

    } catch (error) {
      this._logError('AUTHSERVICE: API call failed with exception', {
        url: url,
        fullUrl: `/api${url}`,
        error: error.message,
        stack: error.stack
      });

      // Zaloguj jako error event
      if (this.logger) {
        this.logger.error('API_CALL_EXCEPTION', 'API call failed with network/other error', {
          url: url,
          fullUrl: `/api${url}`,
          method: options.method || 'GET',
          error: error.message,
          timestamp: new Date().toISOString()
        });
      }

      throw error;
    }
  }
}

// Export singleton instance
export const authService = new AuthService();
export default authService;