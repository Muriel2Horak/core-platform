// Logger service pro aplikaci
// Poskytuje strukturované logování pro různé typy událostí

class Logger {
  constructor() {
    this.initialized = false;
    this.userInfo = null;
    this.sessionId = this.generateSessionId();
    this.startTime = Date.now();
    
    // Konfigurace podle prostředí  
    this.isDevelopment = import.meta.env.DEV;
    this.logLevel = this.isDevelopment ? 'DEBUG' : 'INFO';
    
    // Backend endpoint pro Loki logging
    this.backendLogUrl = '/api/frontend-logs';
    this.enabled = true;
    
    console.log('🔧 Logger initialized:', {
      sessionId: this.sessionId,
      environment: import.meta.env.MODE,
      logLevel: this.logLevel,
      backendLogUrl: this.backendLogUrl
    });
  }

  /**
   * 📊 Inicializace logger s user info
   */
  async initializeUserInfo(userInfo) {
    try {
      this.userInfo = userInfo;
      this.initialized = true;
      
      this.info('LOGGER_INITIALIZED', 'Logger initialized with user info', {
        username: userInfo?.username,
        roles: userInfo?.roles,
        tenant: userInfo?.tenant
      });
    } catch (error) {
      console.error('Failed to initialize user info for logging:', error);
    }
  }

  /**
   * 🆔 Generování session ID
   */
  generateSessionId() {
    return 'sess_' + Math.random().toString(36).substr(2, 9) + '_' + Date.now();
  }

  /**
   * 📝 Základní info log
   */
  info(event, message, extra = {}) {
    this.logEvent('INFO', event, message, extra);
  }

  /**
   * ⚠️ Warning log  
   */
  warn(event, message, extra = {}) {
    this.logEvent('WARN', event, message, extra);
  }

  /**
   * ❌ Error log
   */
  error(event, message, extra = {}) {
    this.logEvent('ERROR', event, message, extra);
  }

  /**
   * 🐛 Debug log (pouze v development)
   */
  debug(event, message, extra = {}) {
    if (this.isDevelopment) {
      this.logEvent('DEBUG', event, message, extra);
    }
  }

  /**
   * 👤 User action log
   */
  userAction(action, details = {}) {
    this.logEvent('USER_ACTION', action, `User performed action: ${action}`, {
      action,
      ...details,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 📄 Page view log
   */
  pageView(page, details = {}) {
    this.logEvent('PAGE_VIEW', 'PAGE_NAVIGATION', `User viewed page: ${page}`, {
      page,
      url: window.location.href,
      referrer: document.referrer,
      ...details,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 🌐 API call log
   */
  apiCall(method, endpoint, status, duration, details = {}) {
    const isError = status >= 400;
    const level = isError ? 'ERROR' : 'INFO';
    
    this.logEvent(level, 'API_CALL', `API ${method} ${endpoint} - ${status}`, {
      method,
      endpoint, 
      status,
      duration,
      success: !isError,
      ...details,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 🔐 Auth event log
   */
  authEvent(event, details = {}) {
    this.logEvent('AUTH', event, `Authentication event: ${event}`, {
      event,
      ...details,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 📊 Performance log
   */
  performance(metric, value, details = {}) {
    this.logEvent('PERFORMANCE', metric, `Performance metric: ${metric} = ${value}`, {
      metric,
      value,
      ...details,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 🎯 Core logging method
   */
  logEvent(level, event, message, extra = {}) {
    const logEntry = {
      level,
      event,
      message,
      timestamp: new Date().toISOString(),
      sessionId: this.sessionId,
      user: this.userInfo ? {
        username: this.userInfo.username,
        tenant: this.userInfo.tenant,
        roles: this.userInfo.roles
      } : null,
      browser: {
        userAgent: navigator.userAgent,
        url: window.location.href,
        viewport: `${window.innerWidth}x${window.innerHeight}`
      },
      ...extra
    };

    // Console output v development
    if (this.isDevelopment || level === 'ERROR') {
      const emoji = this.getLevelEmoji(level);
      const style = this.getLevelStyle(level);
      
      console.groupCollapsed(`${emoji} [${level}] ${event}: ${message}`);
      console.log('%c' + JSON.stringify(logEntry, null, 2), style);
      console.groupEnd();
    }

    // Odešli logy na server (v development i production pro testování)
    // Původně: if (!this.isDevelopment) 
    // Změněno: odesílej vždy když je logger povolen
    if (this.enabled) {
      this.sendToLoggingService(logEntry);
    }
  }

  /**
   * 🎨 Styling pro console logy
   */
  getLevelEmoji(level) {
    const emojis = {
      'DEBUG': '🐛',
      'INFO': 'ℹ️',
      'WARN': '⚠️', 
      'ERROR': '❌',
      'USER_ACTION': '👤',
      'PAGE_VIEW': '📄',
      'API_CALL': '🌐',
      'AUTH': '🔐',
      'PERFORMANCE': '📊'
    };
    return emojis[level] || 'ℹ️';
  }

  getLevelStyle(level) {
    const styles = {
      'DEBUG': 'color: #6c757d; font-size: 11px;',
      'INFO': 'color: #0d6efd; font-size: 12px;',  
      'WARN': 'color: #fd7e14; font-size: 12px;',
      'ERROR': 'color: #dc3545; font-size: 12px; font-weight: bold;',
      'USER_ACTION': 'color: #198754; font-size: 12px;',
      'PAGE_VIEW': 'color: #6f42c1; font-size: 12px;',
      'API_CALL': 'color: #0dcaf0; font-size: 12px;',
      'AUTH': 'color: #fd7e14; font-size: 12px;',
      'PERFORMANCE': 'color: #20c997; font-size: 12px;'
    };
    return styles[level] || styles['INFO'];
  }

  /**
   * 📤 Odeslání logů na server (v produkci)
   */
  async sendToLoggingService(logEntry) {
    if (!this.enabled) return;

    try {
      console.log('📤 LOGGER: Odesílám log na backend...', logEntry);
      
      // Strukturovaný log podle původní implementace + rozšíření
      const structuredLog = {
        timestamp: logEntry.timestamp,
        level: logEntry.level.toUpperCase(),
        service: 'core-platform-frontend',
        message: logEntry.message,
        url: window.location.href,
        userAgent: navigator.userAgent.substring(0, 100),
        sessionId: logEntry.sessionId,
        event: logEntry.event,
        
        // ✨ Rozšíření o požadované pole
        tenant: this.userInfo?.tenant || logEntry.tenant || null,
        username: this.userInfo?.username || logEntry.username || null,
        operation: logEntry.operation || logEntry.event || null,
        page: this.extractPageFromUrl(window.location.href),
        context: {
          component: logEntry.component || null,
          category: logEntry.category || null,
          action: logEntry.action || null,
          ...logEntry.context
        },
        result: this.determineResult(logEntry),
        
        // Původní user objekt pro zpětnou kompatibilitu
        user: logEntry.user || (this.userInfo ? {
          username: this.userInfo.username,
          tenant: this.userInfo.tenant,
          roles: this.userInfo.roles
        } : null),
        
        // HTTP specifické informace
        http: logEntry.method ? {
          method: logEntry.method,
          endpoint: logEntry.endpoint,
          status: logEntry.status,
          duration: logEntry.duration
        } : null,
        
        ...logEntry
      };

      console.log('📡 LOGGER: POST request na', this.backendLogUrl);
      const response = await fetch(this.backendLogUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(structuredLog)
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

  /**
   * 🌍 Extrakce názvu stránky z URL
   */
  extractPageFromUrl(url) {
    try {
      const urlObj = new URL(url);
      const pathname = urlObj.pathname;
      
      // Mapování cest na názvy stránek
      const pageMapping = {
        '/': 'dashboard',
        '/dashboard': 'dashboard',
        '/profile': 'profile',
        '/admin/users': 'user_management',
        '/admin/tenants': 'tenant_management',
        '/directory': 'user_directory',
        '/utilities/typography': 'typography',
        '/utilities/shadow': 'shadow',
        '/test/keycloak': 'keycloak_test'
      };
      
      // Dynamické stránky s parametry
      if (pathname.startsWith('/directory/')) {
        return 'user_detail';
      }
      
      return pageMapping[pathname] || pathname.replace(/^\//, '').replace(/\//g, '_') || 'unknown';
    } catch {
      return 'unknown';
    }
  }

  /**
   * ✅ Určení výsledku operace
   */
  determineResult(logEntry) {
    // HTTP status kódy
    if (logEntry.status) {
      if (logEntry.status >= 200 && logEntry.status < 300) return 'SUCCESS';
      if (logEntry.status >= 400 && logEntry.status < 500) return 'CLIENT_ERROR';
      if (logEntry.status >= 500) return 'SERVER_ERROR';
      return 'UNKNOWN';
    }
    
    // Explicitní success flag
    if (logEntry.success === true) return 'SUCCESS';
    if (logEntry.success === false) return 'FAILURE';
    
    // Level-based result
    if (logEntry.level === 'ERROR') return 'FAILURE';
    if (logEntry.level === 'WARN') return 'WARNING';
    if (logEntry.level === 'INFO' || logEntry.level === 'USER_ACTION') return 'SUCCESS';
    
    return 'INFO';
  }

  /**
   * 📥 Buffering logů pro batch odeslání
   */
  bufferLog(logEntry) {
    if (!this.logBuffer) {
      this.logBuffer = [];
    }
    
    this.logBuffer.push(logEntry);
    
    // Pošli batch každých 50 logů nebo každé 2 minuty
    if (this.logBuffer.length >= 50) {
      this.flushLogBuffer();
    } else if (!this.flushTimer) {
      this.flushTimer = setTimeout(() => {
        this.flushLogBuffer();
      }, 120000); // 2 minuty
    }
  }

  /**
   * 🚿 Flush log buffer
   */
  async flushLogBuffer() {
    if (!this.logBuffer || this.logBuffer.length === 0) return;
    
    try {
      // Odešli každý log jednotlivě přes sendToLoggingService
      for (const logEntry of this.logBuffer) {
        await this.sendToLoggingService(logEntry);
      }
      
      this.logBuffer = [];
      if (this.flushTimer) {
        clearTimeout(this.flushTimer);
        this.flushTimer = null;
      }
    } catch (error) {
      console.error('Failed to flush log buffer:', error);
    }
  }

  /**
   * 🎫 Získání auth tokenu
   */
  getAuthToken() {
    // V reálné aplikaci by se získal z auth service
    return localStorage.getItem('keycloak-token') || '';
  }

  /**
   * 📈 Session statistiky
   */
  getSessionStats() {
    return {
      sessionId: this.sessionId,
      duration: Date.now() - this.startTime,
      userInfo: this.userInfo,
      initialized: this.initialized,
      bufferSize: this.logBuffer?.length || 0
    };
  }
}

// Singleton instance
const logger = new Logger();

// Export default instance
export default logger;

// Export také class pro custom instances
export { Logger };