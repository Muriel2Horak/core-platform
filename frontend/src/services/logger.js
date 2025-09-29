// Logger service pro aplikaci
// Poskytuje strukturované logování pro různé typy událostí

class Logger {
  constructor() {
    this.initialized = false;
    this.userInfo = null;
    this.sessionId = this.generateSessionId();
    this.startTime = Date.now();
    
    // Konfigurace podle prostředí  
    this.isDevelopment = process.env.NODE_ENV === 'development';
    this.logLevel = this.isDevelopment ? 'DEBUG' : 'INFO';
    
    console.log('🔧 Logger initialized:', {
      sessionId: this.sessionId,
      environment: process.env.NODE_ENV,
      logLevel: this.logLevel
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

    // V produkci by zde bylo odeslání na logging server
    if (!this.isDevelopment) {
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
    try {
      // V produkci by zde bylo volání na logging API
      // Například Loki, ELK stack, nebo jiný logging service
      
      if (logEntry.level === 'ERROR') {
        // Kritické chyby pošli okamžitě 
        await fetch('/api/logs/error', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.getAuthToken()}`
          },
          body: JSON.stringify(logEntry)
        });
      } else {
        // Ostatní logy můžeme batchovat
        this.bufferLog(logEntry);
      }
    } catch (error) {
      // Nesmíme způsobit crash aplikace kvůli logování
      console.error('Failed to send log to service:', error);
    }
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
      await fetch('/api/logs/batch', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify({
          logs: this.logBuffer,
          sessionId: this.sessionId
        })
      });
      
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