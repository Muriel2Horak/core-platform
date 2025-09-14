// Structured logger pro frontend - loguje do console A posílá na backend/Loki
class FrontendLogger {
  constructor() {
    this.service = 'core-platform-frontend';
    this.enabled = true;
    
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
    
    // POUZE backend endpoint - backend se postará o přeposlání do Loki
    this.backendLogUrl = '/api/logs/frontend';
    
    console.log(`🔧 LOGGER: Inicializace ${this.isProduction ? 'PRODUCTION' : 'DEVELOPMENT'} módu`);
    console.log(`🔧 LOGGER: Backend URL: ${this.backendLogUrl}`);
    console.log(`🔧 LOGGER: Min log level: ${this.minLogLevel}`);
  }

  // Získání user info pro logy
  _getUserInfo() {
    // Pokusíme se získat info o uživateli z localStorage nebo context
    const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
    return {
      userId: userInfo.id || 'anonymous',
      login: userInfo.preferred_username || userInfo.email || 'anonymous',
      roles: userInfo.roles || []
    };
  }

  // Získání client info
  _getClientInfo() {
    return {
      clientIp: 'unknown', // Frontend nemůže přímo získat real IP
      userAgent: navigator.userAgent.substring(0, 200),
      url: window.location.href,
      referrer: document.referrer || '',
      sessionId: sessionStorage.getItem('sessionId') || 'unknown'
    };
  }

  // Strukturovaný log podle našich požadavků
  _logStructured(level, operation, message, details = {}) {
    const levelNum = this.logLevels[level.toUpperCase()];
    
    // Kontrola min log level
    if (levelNum < this.minLogLevel) {
      return;
    }

    const userInfo = this._getUserInfo();
    const clientInfo = this._getClientInfo();
    
    const logEntry = {
      // Hlavní struktura podle požadavků
      timestamp: new Date().toISOString(),
      level: level.toUpperCase(),
      clientIp: clientInfo.clientIp,
      login: userInfo.login,
      userId: userInfo.userId,
      operation: operation,
      message: message,
      
      // Dodatečné detaily
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

    // 1. Log do console pro developer (local debugging)
    this._logToConsole(level, operation, message, logEntry);
    
    // 2. Pošli na server pro centralizované logování
    this._sendToServer(logEntry);
  }

  // Console logging s emoji
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

  // Zjednodušené odesílání logů - pouze na backend
  async _sendToServer(logEntry) {
    if (!this.enabled) return;

    await this._sendToBackend(logEntry);
  }

  // Helper pro emoji podle log level
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

  // Odeslání logu na backend
  async _sendToBackend(logEntry) {
    try {
      const response = await fetch(this.backendLogUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(logEntry)
      });
      
      if (!response.ok) {
        console.warn('⚠️ LOGGER: Backend odpověděl s chybou:', response.status);
      }
    } catch (error) {
      console.error('🔴 LOGGER: Chyba při odesílání na backend:', error);
      // V případě chyby je to ok - logy jsou nice-to-have, ne kritické
    }
  }

  // Standardní log levely
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

  // Bezpečnostní a audit logy
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

  // Speciální metody pro různé operace

  // Authentication operations
  authLogin(success, details = {}) {
    const level = success ? 'AUDIT' : 'SECURITY';
    const operation = 'AUTH_LOGIN';
    const message = success ? 'User login successful' : 'User login failed';
    
    this._logStructured(level, operation, message, {
      ...details,
      auth_result: success ? 'success' : 'failure',
      category: 'authentication'
    });
  }

  authLogout(details = {}) {
    this._logStructured('AUDIT', 'AUTH_LOGOUT', 'User logout', {
      ...details,
      category: 'authentication'
    });
  }

  authPasswordChange(success, details = {}) {
    const level = success ? 'AUDIT' : 'SECURITY';
    const message = success ? 'Password change successful' : 'Password change failed';
    
    this._logStructured(level, 'AUTH_PASSWORD_CHANGE', message, {
      ...details,
      auth_result: success ? 'success' : 'failure',
      category: 'authentication'
    });
  }

  // API call logging
  apiCall(method, url, status, duration, details = {}) {
    const level = status >= 500 ? 'ERROR' : 
                  status >= 400 ? 'WARN' : 'INFO';
    const operation = 'API_CALL';
    const message = `${method} ${url} -> ${status} (${duration}ms)`;
    
    this._logStructured(level, operation, message, {
      ...details,
      http_method: method,
      http_url: url,
      http_status: status,
      response_time_ms: duration,
      category: 'api'
    });
  }

  // User action logging
  userAction(action, details = {}) {
    this._logStructured('INFO', 'USER_ACTION', action, {
      ...details,
      category: 'user_interaction'
    });
  }

  // Security events
  securityViolation(violation, details = {}) {
    this._logStructured('SECURITY', 'SECURITY_VIOLATION', violation, {
      ...details,
      category: 'security_violation',
      requires_immediate_attention: true
    });
  }

  // Page navigation
  pageView(page, details = {}) {
    this._logStructured('DEBUG', 'PAGE_VIEW', `Navigated to ${page}`, {
      ...details,
      page: page,
      category: 'navigation'
    });
  }
}

// Export singleton instance
export const logger = new FrontendLogger();
export default logger;