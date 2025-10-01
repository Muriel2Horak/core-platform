// Auth utility pro správu JWT tokenů a API volání
class AuthService {
  constructor() {
    const token = localStorage.getItem('keycloak-token');
    console.log('🔧 AUTHSERVICE: Inicializace', { hasToken: !!token });
    
    // 🔧 OPRAVENO: Poslouchej na keycloak-authenticated event
    window.addEventListener('keycloak-authenticated', (event) => {
      console.log('🎉 AuthService: Received keycloak-authenticated event', event.detail);
      // Refresh user info při autentizaci
      this.refreshUserInfo();
    });
  }

  // 🔧 FIX: Zjednodušené logování bez importu loggeru (kvůli cyklické závislosti)
  /**
   * @param {string} message
   * @param {object} extra
   */
  _log(message, extra = {}) {
    console.log(`🔧 AUTHSERVICE: ${message}`, extra);
  }

  /**
   * @param {string} message  
   * @param {object} extra
   */
  _logError(message, extra = {}) {
    console.error(`❌ AUTHSERVICE: ${message}`, extra);
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

  /**
   * 🔄 Refresh user info z localStorage
   */
  refreshUserInfo() {
    try {
      const userInfoStr = localStorage.getItem('keycloak-user-info');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        console.log('🔄 AuthService: User info refreshed', userInfo);
        return userInfo;
      }
    } catch (error) {
      console.warn('Failed to parse user info from localStorage:', error);
    }
    return null;
  }

  /**
   * 👤 Získání informací o aktuálním uživateli
   */
  getUserInfo() {
    try {
      const token = localStorage.getItem('keycloak-token');
      if (!token) {
        console.log('🔍 AUTHSERVICE: No token found');
        return null;
      }

      // 🔧 OPRAVENO: Nejdříve zkus načíst z localStorage (kde ukládá keycloakService)
      const userInfoStr = localStorage.getItem('keycloak-user-info');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        console.log('✅ AUTHSERVICE: User info loaded from localStorage', userInfo);
        return userInfo;
      }

      // 🔧 Fallback: Zkus parsovat z tokenu (původní logika)
      const payload = JSON.parse(atob(token.split('.')[1]));
      
      const userInfo = {
        username: payload.preferred_username,
        email: payload.email,
        firstName: payload.given_name,
        lastName: payload.family_name,
        roles: payload.realm_access?.roles || [],
        tenant: payload.tenant
      };
      
      console.log('✅ AUTHSERVICE: User info extracted from token', userInfo);
      return userInfo;
    } catch (error) {
      console.error('❌ AUTHSERVICE: Error getting user info', error);
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
    
    // Získáme user info před odhlášením pro log
    const userInfo = await this.getUserInfo();
    
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      });
      
      // 🔧 FIX: Async load logger pro audit log
      try {
        const loggerModule = await import('./logger.js');
        loggerModule.default.authLogout({
          username: userInfo?.username || 'unknown',
          method: 'manual',
          timestamp: new Date().toISOString()
        });
      } catch {
        console.log('📋 Auth logout logged:', userInfo?.username || 'unknown');
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
      window.location.href = '/login';
    }
  }

  /**
   * API volání s automatickým přidáním tokenu
   * @param {string} url - API endpoint URL
   * @param {object} options - Fetch options
   */
  async apiCall(url, options = {}) {
    let token = null;
    let tokenSource = 'localStorage'; // default
    
    // 🔧 FIX: Priorita tokenu - vždy zkus keycloakService PRVNÍ
    try {
      const ksModule = await import('./keycloakService');
      const ks = ksModule.default;
      if (ks?.keycloak && ks.isAuthenticated()) {
        try {
          // Vždy obnovit token před voláním
          await ks.keycloak.updateToken(30);
          const freshToken = ks.getToken();
          if (freshToken) {
            token = freshToken;
            tokenSource = 'keycloakService'; // 🔧 FIX: Správně označit zdroj
            // Synchronizuj do localStorage pro backup
            localStorage.setItem('keycloak-token', freshToken);
            this.token = freshToken;
          }
        } catch (e) {
          this._logError('AUTHSERVICE: updateToken před voláním selhal', { error: e?.message });
          // Fallback na localStorage token
          token = this.getToken();
          tokenSource = 'localStorage-fallback';
        }
      } else {
        // keycloakService není autentizovaný, použij localStorage
        token = this.getToken();
        tokenSource = 'localStorage-notauth';
      }
    } catch {
      // keycloakService nemusí být dostupný – použijeme localStorage fallback
      token = this.getToken();
      tokenSource = 'localStorage-noservice';
    }

    // 🔧 FIX: Bezpečný merge hlaviček - vytvoř base Headers a pak merge options.headers
    const headers = new Headers();
    
    // Nejdříve nastav base hlavičky
    headers.set('Content-Type', 'application/json');
    
    // Přidej Authorization token pokud je dostupný
    if (token) {
      // 🔧 DEBUG: Log token pro diagnostiku "Invalid JWT format"
      console.log('🔍 DEBUG: Sending token to backend:', {
        tokenLength: token.length,
        tokenStart: token.substring(0, 50),
        tokenParts: token.split('.').length,
        tokenSource: tokenSource,
        isValidJWT: token.split('.').length === 3
      });
      
      // Zkus parsovat token pro validaci
      try {
        const parts = token.split('.');
        if (parts.length !== 3) {
          console.error('❌ Invalid JWT: Wrong number of parts', parts.length);
        } else {
          const header = JSON.parse(atob(parts[0]));
          const payload = JSON.parse(atob(parts[1]));
          console.log('✅ JWT Header:', header);
          console.log('✅ JWT Payload preview:', {
            sub: payload.sub,
            iss: payload.iss,
            exp: new Date(payload.exp * 1000),
            preferred_username: payload.preferred_username
          });
        }
      } catch (parseError) {
        console.error('❌ Token parsing failed:', parseError);
      }
      
      headers.set('Authorization', `Bearer ${token}`);
    }
    
    // Pak projdi options.headers a nastav je (ale nepřepiš Authorization)
    if (options.headers) {
      for (const [key, value] of Object.entries(options.headers)) {
        if (key.toLowerCase() !== 'authorization' || !token) {
          headers.set(key, value);
        }
      }
    }

    const fullUrl = `/api${url}`; // relativně přes Nginx proxy

    // 🔍 DEBUG: Log finálních hlaviček
    console.debug('AUTH FINAL HEADERS', Array.from(headers.entries()));

    this._log('AUTHSERVICE: API call start', {
      url: url,
      fullUrl: fullUrl,
      method: options.method || 'GET',
      hasToken: !!token,
      tokenSource: tokenSource, // 🔧 FIX: Používám správnou proměnnou
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
        // Pokus o refresh tokenu a jednorázový retry pouze pokud máme keycloakService
        try {
          const ksModule = await import('./keycloakService');
          const ks = ksModule.default;
          if (ks?.keycloak) {
            const refreshed = await ks.keycloak.updateToken(10);
            if (refreshed) {
              const newToken = ks.getToken();
              if (newToken && newToken !== token) {
                localStorage.setItem('keycloak-token', newToken);
                this.token = newToken;
                
                // 🔧 FIX: Bezpečný retry s novým tokenem
                const retryHeaders = new Headers(headers);
                retryHeaders.set('Authorization', `Bearer ${newToken}`);
                
                this._log('AUTHSERVICE: Retrying with refreshed token');
                response = await fetch(fullUrl, { ...options, headers: retryHeaders, credentials: 'include' });
              }
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

        // 🔧 FIX: Async load logger pro security log
        try {
          const loggerModule = await import('./logger.js');
          loggerModule.default.security('API_401_UNAUTHORIZED', 'API call returned 401 - token invalid or expired', {
            url: url,
            fullUrl: fullUrl,
            method: options.method || 'GET',
            hasToken: !!token,
            timestamp: new Date().toISOString()
          });
        } catch {
          console.warn('🔒 Security event logged:', 'API_401_UNAUTHORIZED');
        }

        this.logout();
        return null;
      }

      return response;

    } catch (/** @type {Error} */ error) {
      this._logError('AUTHSERVICE: API call failed with exception', {
        url: url,
        fullUrl: `/api${url}`,
        error: error.message,
        stack: error.stack
      });

      // 🔧 FIX: Async load logger pro error log
      try {
        const loggerModule = await import('./logger.js');
        loggerModule.default.error('API_CALL_EXCEPTION', 'API call failed with network/other error', {
          url: url,
          fullUrl: `/api${url}`,
          method: options.method || 'GET',
          error: error.message,
          timestamp: new Date().toISOString()
        });
      } catch {
        console.error('❌ Error logged:', error.message);
      }

      throw error;
    }
  }
}

// Export only default instance for consistency
const authService = new AuthService();
export default authService;