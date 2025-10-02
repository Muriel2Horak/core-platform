class FrontendLogger {
  constructor() {
    this.apiUrl = '/api/frontend-logs';  // ✅ Opraveno - odpovídá backend endpointu
    this.queue = [];
    this.isProcessing = false;
    this.tenant = 'unknown';
    this.username = 'anonymous';
    this.isAuthenticated = false; // 🔐 Nový flag pro autentifikaci
    
    // Flush logs every 5 seconds or when queue reaches 10 items
    setInterval(() => this.flush(), 5000);
  }

  // 🔐 Set authentication status
  setAuthenticated(isAuthenticated) {
    this.isAuthenticated = isAuthenticated;
    if (isAuthenticated) {
      // When user gets authenticated, try to flush queued logs
      this.flush();
    } else {
      // Clear any stored tokens when not authenticated
      localStorage.removeItem('keycloak-token');
    }
  }

  // 🏢 Set tenant context (called after authentication)
  setTenantContext(tenant, username) {
    this.tenant = tenant || 'unknown';
    this.username = username || 'anonymous';
    this.isAuthenticated = true; // 🔐 Auto-set as authenticated when context is set
    this.info('🏢 Logger tenant context updated', { 
      tenant: this.tenant, 
      username: this.username 
    });
  }

  log(level, message, context = {}) {
    const logEntry = {
      level: level.toUpperCase(),
      message,
      operation: context.operation || context.component || 'frontend',
      timestamp: new Date().toISOString(),
      tenant: this.tenant,
      username: this.username,
      login: this.username,
      details: {
        url: window.location.href,
        userAgent: navigator.userAgent,
        category: context.category || 'general',
        component: context.component || 'frontend',
        ...context
      }
    };

    // Always log to console - FIX: proper object display
    const consoleMethod = console[level] || console.log;
    if (Object.keys(context).length > 0) {
      consoleMethod(`[${level.toUpperCase()}] [${this.tenant}] [${this.username}] ${message}`, context);
    } else {
      consoleMethod(`[${level.toUpperCase()}] [${this.tenant}] [${this.username}] ${message}`);
    }

    // Only queue logs that should be sent to backend
    if (this.isAuthenticated || level === 'error') {
      this.queue.push(logEntry);
      
      // Auto-flush for errors or auth events
      if (level === 'error' || context.component === 'auth') {
        this.flush();
      }
      
      // Auto-flush when queue is full
      if (this.queue.length >= 10) {
        this.flush();
      }
    }
  }

  debug(message, context) { this.log('debug', message, context); }
  info(message, context) { this.log('info', message, context); }
  warn(message, context) { this.log('warn', message, context); }
  error(message, context) { this.log('error', message, context); }

  // 🔐 Special method for auth-related logs
  auth(message, context = {}) {
    this.info(message, { ...context, component: 'auth' });
  }

  async flush() {
    // 🔐 Don't send logs to backend if not authenticated (except errors)
    if (this.isProcessing || this.queue.length === 0) {
      return;
    }

    // Filter logs - only send if authenticated or if it's an error
    const logsToSend = this.queue.filter(log => 
      this.isAuthenticated || log.level === 'ERROR'
    );
    
    if (logsToSend.length === 0) {
      return;
    }

    this.isProcessing = true;
    // Remove sent logs from queue
    this.queue = this.queue.filter(log => !logsToSend.includes(log));

    try {
      const token = localStorage.getItem('keycloak-token');
      
      // Only send to backend if we have a token or it's an error
      if (!token && !logsToSend.some(log => log.level === 'ERROR')) {
        return;
      }

      for (const logEntry of logsToSend) {
        try {
          const response = await fetch(this.apiUrl, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              ...(token && { 'Authorization': `Bearer ${token}` })
            },
            body: JSON.stringify(logEntry),
            timeout: 5000
          });

          if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
          }
        } catch (error) {
          // If individual log fails, put it back in queue (but limit queue size)
          if (this.queue.length < 50) {
            this.queue.push(logEntry);
          }
          // Don't log to console for 401 errors during non-auth requests
          if (error.message !== 'HTTP 401: Unauthorized' || logEntry.level === 'ERROR') {
            console.warn('⚠️ Failed to send log to backend:', error.message);
          }
        }
      }

    } catch (error) {
      // Put all logs back in queue for retry, but limit queue size
      if (this.queue.length < 50) {
        this.queue.unshift(...logsToSend);
      }
      
      console.warn('⚠️ Failed to send logs to backend:', error.message);
    } finally {
      this.isProcessing = false;
    }
  }

  // Setup global error handling
  setupGlobalErrorHandling() {
    window.addEventListener('error', (event) => {
      this.error('Unhandled JavaScript error', {
        component: 'global-error-handler',
        message: event.message,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        stack: event.error?.stack
      });
    });

    window.addEventListener('unhandledrejection', (event) => {
      this.error('Unhandled Promise rejection', {
        component: 'global-error-handler',
        reason: event.reason?.toString(),
        stack: event.reason?.stack
      });
    });
  }

  // Manual flush for critical logs
  async flushImmediate() {
    await this.flush();
  }
}

// Create singleton instance
const logger = new FrontendLogger();
logger.setupGlobalErrorHandling();

export default logger;