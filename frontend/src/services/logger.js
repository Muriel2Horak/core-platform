import axios from 'axios';

class FrontendLogger {
  constructor() {
    this.apiUrl = '/api/auth/logs'; // ✅ Updated to use new backend endpoint
    this.queue = [];
    this.isProcessing = false;
    this.tenant = 'unknown';
    this.username = 'anonymous';
    
    // Flush logs every 5 seconds or when queue reaches 10 items
    setInterval(() => this.flush(), 5000);
  }

  // 🏢 Set tenant context (called after authentication)
  setTenantContext(tenant, username) {
    this.tenant = tenant || 'unknown';
    this.username = username || 'anonymous';
    this.info('🏢 Logger tenant context updated', { 
      tenant: this.tenant, 
      username: this.username 
    });
  }

  log(level, message, context = {}) {
    const logEntry = {
      level: level.toUpperCase(),
      message,
      component: context.component || 'app',
      timestamp: new Date().toISOString(),
      data: {
        url: window.location.href,
        userAgent: navigator.userAgent,
        tenant: this.tenant,
        username: this.username,
        ...context
      }
    };

    this.queue.push(logEntry);
    
    // Also log to console in development
    if (process.env.NODE_ENV === 'development') {
      const consoleMethod = console[level] || console.log;
      consoleMethod(`[${level.toUpperCase()}] [${this.tenant}] [${this.username}]`, message, context);
    }

    // Flush immediately for errors
    if (level === 'error') {
      this.flush();
    }

    // Auto-flush when queue is full
    if (this.queue.length >= 10) {
      this.flush();
    }
  }

  debug(message, context) { this.log('debug', message, context); }
  info(message, context) { this.log('info', message, context); }
  warn(message, context) { this.log('warn', message, context); }
  error(message, context) { this.log('error', message, context); }

  async flush() {
    if (this.isProcessing || this.queue.length === 0) {
      return;
    }

    this.isProcessing = true;
    const logsToSend = [...this.queue];
    this.queue = [];

    try {
      // Send each log entry individually to backend
      for (const logEntry of logsToSend) {
        try {
          await axios.post(this.apiUrl, logEntry, {
            timeout: 5000,
            headers: {
              'Content-Type': 'application/json'
            }
          });
        } catch (error) {
          // If individual log fails, put it back in queue
          this.queue.push(logEntry);
          
          // Only log to console to avoid infinite loop
          if (process.env.NODE_ENV === 'development') {
            console.error('Failed to send individual frontend log:', error.message);
          }
        }
      }
    } catch (error) {
      // If batch sending fails, put all logs back in queue
      this.queue.unshift(...logsToSend);
      
      // Only log to console to avoid infinite loop
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to send frontend logs batch:', error.message);
      }
    } finally {
      this.isProcessing = false;
    }
  }

  // Log unhandled errors with tenant context
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

    // Log page navigation
    let lastUrl = window.location.href;
    const observer = new MutationObserver(() => {
      if (window.location.href !== lastUrl) {
        this.info('Page navigation', {
          component: 'navigation',
          from: lastUrl,
          to: window.location.href
        });
        lastUrl = window.location.href;
      }
    });
    
    observer.observe(document, { subtree: true, childList: true });
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