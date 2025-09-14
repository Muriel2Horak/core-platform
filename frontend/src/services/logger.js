// Structured logger pro frontend - loguje do console A posílá na backend/Loki
class FrontendLogger {
  constructor() {
    this.service = 'core-platform-frontend';
    this.enabled = true;
    
    // Detekce prostředí a nastavení endpointů
    this.isProduction = import.meta.env.PROD;
    this.isDevelopment = import.meta.env.DEV;
    
    // POUZE backend endpoint - backend se postará o přeposlání do Loki
    this.backendLogUrl = '/api/frontend-logs';
    
    console.log(`🔧 LOGGER: Inicializace ${this.isProduction ? 'PRODUCTION' : 'DEVELOPMENT'} módu`);
    console.log(`🔧 LOGGER: Backend URL: ${this.backendLogUrl}`);
  }

  // Strukturovaný log podle JSON Structured Logging standardu
  _logStructured(level, message, extra = {}) {
    const logEntry = {
      timestamp: new Date().toISOString(),
      level: level.toUpperCase(),
      service: this.service,
      message: message,
      url: window.location.href,
      userAgent: navigator.userAgent.substring(0, 100),
      ...extra
    };

    // 1. Log do console pro developer (local debugging)
    console.log(`${this._getEmoji(level)} ${extra.component?.toUpperCase() || 'FRONTEND'}: ${message}`, extra);
    console.log(JSON.stringify(logEntry));
    
    // 2. Pošli na server pro centralizované logování
    this._sendToServer(logEntry);
  }

  // Zjednodušené odesílání logů - pouze na backend
  async _sendToServer(logEntry) {
    if (!this.enabled) return;

    console.log('📤 LOGGER: Odesílám log na backend...', logEntry);
    await this._sendToBackend(logEntry);
  }

  // Helper pro emoji podle log level
  _getEmoji(level) {
    switch (level.toLowerCase()) {
      case 'error': return '❌';
      case 'warn': return '⚠️';
      case 'info': return 'ℹ️';
      case 'debug': return '🐛';
      default: return 'ℹ️';
    }
  }

  // Odeslání logu na backend (production + fallback)
  async _sendToBackend(logEntry) {
    try {
      console.log('📡 LOGGER: POST request na', this.backendLogUrl);
      const response = await fetch(this.backendLogUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(logEntry)
      });
      
      if (response.ok) {
        console.log('✅ LOGGER: Log úspěšně odeslán na backend');
      } else {
        console.warn('⚠️ LOGGER: Backend odpověděl s chybou:', response.status);
      }
    } catch (error) {
      console.error('🔴 LOGGER: Chyba při odesílání na backend:', error);
      // V případě chyby je to ok - logy jsou nice-to-have, ne kritické
    }
  }

  // Standardní log levely
  info(message, extra = {}) {
    console.log(`ℹ️ ${message}`, extra); // Readable pro developer
    this._logStructured('info', message, extra);
  }

  warn(message, extra = {}) {
    console.warn(`⚠️ ${message}`, extra);
    this._logStructured('warn', message, extra);
  }

  error(message, extra = {}) {
    console.error(`❌ ${message}`, extra);
    this._logStructured('error', message, extra);
  }

  debug(message, extra = {}) {
    console.log(`🐛 ${message}`, extra);
    this._logStructured('debug', message, extra);
  }

  // Speciální metody pro authentication flow
  auth(message, extra = {}) {
    console.log(`🔐 AUTH: ${message}`, extra);
    this._logStructured('info', `AUTH: ${message}`, { 
      ...extra, 
      component: 'authentication',
      category: 'auth_flow'
    });
  }

  guard(message, extra = {}) {
    console.log(`🛡️ GUARD: ${message}`, extra);
    this._logStructured('info', `GUARD: ${message}`, { 
      ...extra, 
      component: 'auth_guard',
      category: 'auth_flow'
    });
  }

  service(message, extra = {}) {
    console.log(`🔧 SERVICE: ${message}`, extra);
    this._logStructured('info', `SERVICE: ${message}`, { 
      ...extra, 
      component: 'auth_service',
      category: 'auth_flow'
    });
  }

  // API call logging
  api(method, url, status, extra = {}) {
    const level = status >= 400 ? 'error' : status >= 300 ? 'warn' : 'info';
    const message = `API ${method} ${url} -> ${status}`;
    
    console.log(`🌐 ${message}`, extra);
    this._logStructured(level, message, { 
      ...extra,
      component: 'api_client',
      category: 'http_request',
      http_method: method,
      http_url: url,
      http_status: status
    });
  }

  // User action logging
  userAction(action, extra = {}) {
    console.log(`👤 USER: ${action}`, extra);
    this._logStructured('info', `USER: ${action}`, { 
      ...extra,
      component: 'user_interface',
      category: 'user_action'
    });
  }
}

// Export singleton instance
export const logger = new FrontendLogger();
export default logger;