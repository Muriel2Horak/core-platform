// Structured logger pro frontend - loguje do console A posílá na backend/Loki
import authService from './auth';

class FrontendLogger {
  constructor() {
    this.service = 'core-platform-frontend';
    this.enabled = true; // 🔧 ZAPNUTO ZPĚT - opravil jsem tokenSource logiku v auth.js
    
    // Detekce prostředí a nastavení endpointů
    this.isProduction = import.meta.env.PROD;
    this.isDevelopment = import.meta.env.DEV;
    
    // Konfigurace log levelů podle prostředí
    this.logLevels = {
      DEBUG: 0,
      INFO: 1,
      WARN: 2,
      ERROR: 3,
      SECURITY: 4,
      AUDIT: 5
    };
    
    // V produkci logujeme jen od INFO výš, v developmentu vše
    this.minLogLevel = this.isProduction ? this.logLevels.INFO : this.logLevels.DEBUG;
    
    // Backend endpoint - MUSÍ jít přes Nginx proxy, ne přímo na port 8080
    this.backendLogUrl = '/frontend-logs'; // bude prefikxováno v authService.apiCall na /api
  }

  // Získání user info pro logy - opraveno pro keycloak token
  _getUserInfo() {
    try {
      const token = localStorage.getItem('keycloak-token');
      if (!token) {
        return {
          userId: 'anonymous',
          login: 'anonymous',
          roles: []
        };
      }

      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        userId: payload.sid || payload.preferred_username || 'unknown',
        login: payload.preferred_username || payload.email || 'unknown',
        roles: payload.realm_access?.roles || []
      };
    } catch {
      return {
        userId: 'anonymous',
        login: 'anonymous',
        roles: []
      };
    }
  }

  _getClientInfo() {
    const userInfo = this._getUserInfo();
    
    return {
      clientIp: 'unknown',
      userAgent: navigator.userAgent.substring(0, 200),
      url: window.location.href,
      referrer: document.referrer || '',
      sessionId: userInfo.userId !== 'anonymous' ? userInfo.userId : 'unknown'
    };
  }

  _logStructured(level, operation, message, details = {}) {
    const levelNum = this.logLevels[level.toUpperCase()];
    if (levelNum < this.minLogLevel) {
      return;
    }

    const userInfo = this._getUserInfo();
    const clientInfo = this._getClientInfo();
    
    const logEntry = {
      timestamp: new Date().toISOString(),
      level: level.toUpperCase(),
      clientIp: clientInfo.clientIp,
      login: userInfo.login,
      userId: userInfo.userId,
      operation: operation,
      message: message,
      
      details: {
        service: this.service,
        url: clientInfo.url,
        userAgent: clientInfo.userAgent,
        referrer: clientInfo.referrer,
        sessionId: clientInfo.sessionId,
        roles: userInfo.roles,
        ...details
      }
    };

    this._logToConsole(level, operation, message, logEntry);
    this._sendToServer(logEntry);
  }

  _logToConsole(level, operation, message, logEntry) {
    const emoji = this._getEmoji(level);
    const prefix = `${emoji} [${level}] ${operation}:`;
    
    switch (level.toLowerCase()) {
      case 'error':
      case 'security':
        console.error(prefix, message, logEntry.details);
        break;
      case 'warn':
        console.warn(prefix, message, logEntry.details);
        break;
      case 'debug':
        console.debug(prefix, message, logEntry.details);
        break;
      default:
        console.log(prefix, message, logEntry.details);
    }
  }

  async _sendToServer(logEntry) {
    if (!this.enabled) return;
    try {
      const response = await authService.apiCall(this.backendLogUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(logEntry)
      });

      if (!response || !response.ok) {
        console.warn('⚠️ LOGGER: Backend odpověděl s chybou:', response?.status);
      }
    } catch (error) {
      console.error('🔴 LOGGER: Chyba při odesílání na backend:', error);
    }
  }

  _getEmoji(level) {
    switch (level.toLowerCase()) {
      case 'error': return '❌';
      case 'warn': return '⚠️';
      case 'info': return 'ℹ️';
      case 'debug': return '🐛';
      case 'security': return '🔒';
      case 'audit': return '📋';
      default: return 'ℹ️';
    }
  }

  // Veřejné API
  debug(operation, message, details = {}) {
    this._logStructured('DEBUG', operation, message, details);
  }

  info(operation, message, details = {}) {
    this._logStructured('INFO', operation, message, details);
  }

  warn(operation, message, details = {}) {
    this._logStructured('WARN', operation, message, details);
  }

  error(operation, message, details = {}) {
    this._logStructured('ERROR', operation, message, details);
  }

  security(operation, message, details = {}) {
    this._logStructured('SECURITY', operation, message, {
      ...details,
      category: 'security',
      requires_attention: true
    });
  }

  audit(operation, message, details = {}) {
    this._logStructured('AUDIT', operation, message, {
      ...details,
      category: 'audit',
      compliance: true
    });
  }

  userAction(action, details = {}) {
    this._logStructured('INFO', 'USER_ACTION', action, {
      ...details,
      category: 'user_interaction'
    });
  }

  securityViolation(violation, details = {}) {
    this._logStructured('SECURITY', 'SECURITY_VIOLATION', violation, {
      ...details,
      category: 'security_violation',
      requires_immediate_attention: true
    });
  }

  pageView(page, details = {}) {
    this._logStructured('DEBUG', 'PAGE_VIEW', `Navigated to ${page}`, {
      ...details,
      page: page,
      category: 'navigation'
    });
  }

  // ✅ Přidáno: Kompatibilní metoda pro logování API volání používaná v ostatních službách
  //   - signature: apiCall(method, url, statusOrStart, duration, details)
  //   - neprovádí žádné přímé HTTP requesty kromě odeslání logu přes authService.apiCall
  apiCall(method, url, statusOrStart, duration = 0, details = {}) {
    try {
      const isStart = typeof statusOrStart === 'string' && statusOrStart.toLowerCase() === 'start';
      const statusNum = !isStart && typeof statusOrStart === 'number' ? statusOrStart : null;

      let level = 'INFO';
      if (isStart) level = 'DEBUG';
      else if (statusNum !== null) {
        if (statusNum >= 500) level = 'ERROR';
        else if (statusNum >= 400) level = 'WARN';
        else level = 'INFO';
      }

      const message = isStart
        ? `API ${method} ${url} start`
        : `API ${method} ${url} finished${statusNum !== null ? ` (${statusNum})` : ''}`;

      // Nepřepisujeme existující details, pouze doplňujeme standardizovaná pole
      this._logStructured(level, 'API_CALL', message, {
        category: 'api',
        method: method,
        request_url: url,
        http_status: statusNum ?? undefined,
        duration: duration || undefined,
        ...details
      });
    } catch (e) {
      // Bezpečný fallback, aby logování nerozbilo aplikaci
      console.warn('LOGGER.apiCall fallback', e);
    }
  }
}

// Export singleton instance
export const logger = new FrontendLogger();
export default logger;