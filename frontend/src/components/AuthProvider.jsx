import { createContext, useContext, useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
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
        
        // 🔧 FIXED: Always initialize Keycloak, but skip auto-login if logged out
        const shouldSkipAutoLogin = currentPath === '/logged-out' || preventAutoLogin || logoutCompleted;
        
        if (shouldSkipAutoLogin) {
          logger.info('🚪 User was logged out, initializing Keycloak without auto-login');
        }

        // 🔧 Always initialize Keycloak (needed for manual login)
        const keycloakInstance = await keycloakService.initKeycloakOnce();
        
        if (keycloakInstance && keycloakInstance.authenticated) {
          logger.info('✅ User is authenticated');
          
          // Set token in API service first
          const token = keycloakService.getToken();
          await apiService.createSession(token);
          
          try {
            // 🔧 FIXED: Load complete user data from /api/me instead of just JWT data
            const completeUserData = await apiService.getMe();
            
            // Get basic info from JWT as fallback
            const jwtUserInfo = keycloakService.getUserInfo();
            
            // 🔍 DEBUG: Log raw data
            console.log('🔍 DEBUG: JWT user info:', jwtUserInfo);
            console.log('🔍 DEBUG: API user data:', completeUserData);
            
            // 🔧 FIXED: Merge with API data priority (API has tenant from TenantContext)
            const userInfo = {
              ...jwtUserInfo,
              ...completeUserData,
              // Ensure critical fields - API tenant is from TenantContext (more reliable)
              roles: jwtUserInfo?.roles || completeUserData?.roles || [],
              tenant: completeUserData?.tenant || jwtUserInfo?.tenant  // 🔧 API tenant má prioritu (z TenantContext)
            };
            
            // 🔍 DEBUG: Log merged user info
            console.log('🔍 DEBUG: Merged user info:', userInfo);
            console.log('🔍 DEBUG: Final roles:', userInfo.roles);
            
            setUser(userInfo);
            setIsAuthenticated(true);
            
            logger.info('✅ User session established with complete data', {
              username: userInfo.username,
              tenant: userInfo.tenant,
              roles: userInfo.roles, // 🔍 Přidáno do logu
              tenantSource: completeUserData?.tenant ? 'API (TenantContext)' : 'JWT',
              hasCompletData: !!completeUserData
            });
          } catch (error) {
            // Fallback to JWT data if API call fails
            logger.warn('Failed to load complete user data, using JWT fallback', { error: error.message });
            const userInfo = keycloakService.getUserInfo();
            if (userInfo) {
              setUser(userInfo);
              setIsAuthenticated(true);
              
              logger.info('✅ User session established with JWT fallback', {
                username: userInfo.username,
                tenant: userInfo.tenant
              });
            }
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
        // 🔐 FIXED: Use console.error instead of logger.error to avoid auth loops
        console.error('❌ [AUTH] Auth initialization failed:', error.message);
        setError(`Chyba při inicializaci: ${error.message}`);
      } finally {
        setLoading(false);
        isInitializingRef.current = false;
      }
    };

    initializeAuth();
  }, []); // 🔧 Prázdné dependencies - spustí se pouze jednou

  // 🔧 Handle manual login s ref guard
  const handleLogin = async () => {
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
      // 🔧 FIXED: Ensure Keycloak is initialized before login attempt
      if (!keycloakInitialized || !keycloakService.keycloak) {
        logger.info('⏳ Keycloak not ready, initializing first...');
        await keycloakService.initKeycloakOnce();
        setKeycloakInitialized(true);
      }
      
      keycloakService.login();
    } catch (error) {
      // 🔐 FIXED: Use console.error instead of logger.error to avoid auth loops
      console.error('❌ [AUTH] Login failed:', error.message);
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
      // 🔐 FIXED: Use console.error instead of logger.error to avoid auth loops
      console.error('❌ [AUTH] Logout failed:', error.message);
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

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired,
};