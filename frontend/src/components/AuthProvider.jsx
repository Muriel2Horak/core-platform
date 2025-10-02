import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import keycloakService from '../services/keycloakService.js';
import apiService from '../services/api.js';
import logger from '../services/logger.js';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);
  const [showLoggedOut, setShowLoggedOut] = useState(false);
  
  // 🔧 REF GUARDS - prevence duplicitních operací
  const hasTriedLoginRef = useRef(false);
  const isInitializingRef = useRef(false);

  // 🔧 Inicializace pouze jednou při mount
  useEffect(() => {
    const initializeAuth = async () => {
      if (isInitializingRef.current) {
        logger.debug('Auth initialization already in progress, skipping...');
        return;
      }

      isInitializingRef.current = true;

      try {
        setLoading(true);
        logger.info('🔧 Initializing authentication...');
        
        // Check if user was recently logged out
        const logoutCompleted = localStorage.getItem('logout-completed');
        const preventAutoLogin = localStorage.getItem('prevent-auto-login') === 'true';
        const currentPath = window.location.pathname;
        
        // If we're on logged-out page or logout flags are set, don't auto-login
        if (currentPath === '/logged-out' || preventAutoLogin || logoutCompleted) {
          logger.info('🚪 User was logged out, showing login option');
          setKeycloakInitialized(true);
          setIsAuthenticated(false);
          setShowLoggedOut(true);
          setLoading(false);
          return;
        }

        // 🔧 Use singleton initialization
        const keycloakInstance = await keycloakService.initKeycloakOnce();
        
        if (keycloakInstance && keycloakInstance.authenticated) {
          logger.info('✅ User is authenticated');
          
          // Get user info
          const userInfo = keycloakService.getUserInfo();
          if (userInfo) {
            setUser(userInfo);
            setIsAuthenticated(true);
            
            // Set token in API service
            const token = keycloakService.getToken();
            await apiService.createSession(token);
            
            logger.info('✅ User session established', {
              username: userInfo.username,
              tenant: userInfo.tenant
            });
          }
          
          // Clear logout flags on successful authentication
          localStorage.removeItem('logout-completed');
          localStorage.removeItem('prevent-auto-login');
        } else {
          logger.info('ℹ️ User not authenticated - ready for manual login');
          setIsAuthenticated(false);
          setShowLoggedOut(true);
        }
        
        setKeycloakInitialized(true);
      } catch (error) {
        logger.error('❌ Auth initialization failed', { 
          error: error.message,
          stack: error.stack 
        });
        setError(`Chyba při inicializaci: ${error.message}`);
      } finally {
        setLoading(false);
        isInitializingRef.current = false;
      }
    };

    initializeAuth();
  }, []); // 🔧 Prázdné dependencies - spustí se pouze jednou

  // 🔧 Handle manual login s ref guard
  const handleLogin = () => {
    if (hasTriedLoginRef.current) {
      logger.debug('Login already attempted, ignoring duplicate call');
      return;
    }

    hasTriedLoginRef.current = true;
    logger.info('🔐 Manual login initiated');
    
    // Clear logout flags
    localStorage.removeItem('logout-completed');
    localStorage.removeItem('prevent-auto-login');
    
    try {
      keycloakService.login();
    } catch (error) {
      logger.error('Login failed', { error: error.message });
      hasTriedLoginRef.current = false; // Reset on error
    }
  };

  // Handle logout
  const handleLogout = async () => {
    try {
      logger.info('🚪 Logout initiated');
      
      // Set logout flags
      localStorage.setItem('logout-completed', Date.now().toString());
      localStorage.setItem('prevent-auto-login', 'true');
      
      // Clear API session
      await apiService.logout();
      
      // Clear state
      setUser(null);
      setIsAuthenticated(false);
      setShowLoggedOut(true);
      
      // Reset ref guards
      hasTriedLoginRef.current = false;
      
      // Logout from Keycloak
      await keycloakService.logout();
      
    } catch (error) {
      logger.error('❌ Logout failed', { error: error.message });
    }
  };

  const contextValue = {
    loading,
    error,
    user,
    isAuthenticated,
    keycloakInitialized,
    showLoggedOut,
    login: handleLogin,
    logout: handleLogout
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};