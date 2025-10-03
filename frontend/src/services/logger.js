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
      // Clear the queue when user logs out to prevent 401 errors
      this.queue = [];
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

    // 🔐 FIXED: Only queue logs if user is authenticated - no exceptions for errors
    if (this.isAuthenticated) {
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

  // 📄 Page view logging
  pageView(page, context = {}) {
    this.info(`📄 Page view: ${page}`, { 
      ...context, 
      component: 'navigation',
      page,
      category: 'page-view'
    });
  }

  // 🎯 User action logging
  userAction(action, context = {}) {
    this.info(`🎯 User action: ${action}`, { 
      ...context, 
      component: 'user-interaction',
      action,
      category: 'user-action'
    });
  }

  async flush() {
    // 🔐 Early exit if not authenticated, processing, or empty queue
    if (this.isProcessing || this.queue.length === 0 || !this.isAuthenticated) {
      return;
    }

    this.isProcessing = true;
    const logsToSend = [...this.queue]; // Copy all logs to send
    this.queue = []; // Clear queue immediately

    try {
      const token = localStorage.getItem('keycloak-token');
      
      // 🔐 FIXED: Strict check - don't send anything without token
      if (!token) {
        this.setAuthenticated(false);
        return;
      }

      for (const logEntry of logsToSend) {
        try {
          const response = await fetch(this.apiUrl, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(logEntry),
            timeout: 5000
          });

          if (!response.ok) {
            // 🔐 FIXED: Handle 401 specifically to prevent auth loops
            if (response.status === 401) {
              this.setAuthenticated(false);
              return; // Stop processing and exit
            }
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
          }
        } catch (error) {
          // 🔐 FIXED: Handle 401 errors to prevent loops
          if (error.message.includes('401')) {
            this.setAuthenticated(false);
            return;
          }
          
          // For other errors, put the log back in queue (but limit queue size)
          if (this.queue.length < 50) {
            this.queue.push(logEntry);
          }
          
          // Only log non-auth errors to console
          console.warn('⚠️ Failed to send log to backend:', error.message);
        }
      }

    } catch (error) {
      // 🔐 FIXED: Handle auth errors gracefully
      if (error.message.includes('401')) {
        this.setAuthenticated(false);
        return;
      }
      
      // Put all logs back in queue for retry, but limit queue size
      if (this.queue.length + logsToSend.length <= 50) {
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