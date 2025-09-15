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
    } catch (error) {
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
    } catch (error) {
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

  // Získání tokenu pro API volání
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
    const currentUrl = encodeURIComponent(window.location.pathname + window.location.search);
    this._log('AUTHSERVICE: Přesměrovávám na login', { currentUrl });
    window.location.href = `/auth?redirect=${currentUrl}`;
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
      window.location.href = '/auth';
    }
  }

  // API volání s automatickým přidáním tokenu
  async apiCall(url, options = {}) {
    const token = this.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    // Přidej Bearer token pokud existuje
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`/api${url}`, {
      ...options,
      headers,
      credentials: 'include' // Pro cookies kompatibilitu
    });

    if (response.status === 401) {
      // Token vypršel nebo je neplatný
      this._log('AUTHSERVICE: API volání vrátilo 401, odhlašuji');
      this.logout();
      return;
    }

    return response;
  }
}

// Export singleton instance
export const authService = new AuthService();
export default authService;