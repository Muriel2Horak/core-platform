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
  // ❌ REMOVED: showLoggedOut state - nepotřebné, redirect proběhne okamžitě
  
  // 🔧 REF GUARDS - prevence duplicitních operací
  const hasTriedLoginRef = useRef(false);
  const isInitializingRef = useRef(false);
  
  // 🆕 CDC POLLING STATE
  const lastCheckTimestamp = useRef(null);
  const cdcIntervalRef = useRef(null);

  // 🆕 CENTRALIZED USER INFO LOADING
  const loadUserInfo = async () => {
    try {
      logger.info('📥 Loading complete user info...');
      
      // 1. Load complete user data from /api/me (includes tenant from TenantContext)
      const apiUserData = await apiService.getMe();
      
      // 2. Get roles from JWT token (most reliable source for roles)
      const jwtUserInfo = keycloakService.getUserInfo();
      
      // 3. Merge data - roles from JWT, other data from API
      const completeUserInfo = {
        ...apiUserData,
        roles: jwtUserInfo?.roles || apiUserData?.roles || [],
        // Ensure tenant is from API (TenantContext)
        tenant: apiUserData?.tenant || jwtUserInfo?.tenant,
      };
      
      logger.info('✅ Complete user info loaded', {
        username: completeUserInfo.username,
        tenant: completeUserInfo.tenant,
        roles: completeUserInfo.roles,
        rolesCount: completeUserInfo.roles?.length || 0,
        hasApiData: !!apiUserData,
        hasJwtData: !!jwtUserInfo
      });
      
      setUser(completeUserInfo);
      
      // Initialize CDC timestamp
      lastCheckTimestamp.current = Date.now();
      
      return completeUserInfo;
      
    } catch (error) {
      logger.error('❌ Failed to load complete user info', { error: error.message });
      
      // Fallback to JWT only
      const jwtUserInfo = keycloakService.getUserInfo();
      if (jwtUserInfo) {
        logger.warn('⚠️ Using JWT fallback for user info');
        setUser(jwtUserInfo);
        return jwtUserInfo;
      }
      
      throw error;
    }
  };

  // 🆕 CDC POLLING - kontrola změn každých 30s
  const startCdcPolling = () => {
    // Clear any existing interval
    if (cdcIntervalRef.current) {
      clearInterval(cdcIntervalRef.current);
    }
    
    logger.info('🔄 Starting CDC polling (30s interval)');
    
    cdcIntervalRef.current = setInterval(async () => {
      try {
        const since = lastCheckTimestamp.current;
        const changeData = await apiService.checkUserChanges(since);
        
        if (changeData.hasChanges) {
          logger.info('🔔 User data changed, reloading...', {
            lastCheck: since,
            currentTimestamp: changeData.timestamp
          });
          
          await loadUserInfo();
        } else {
          logger.debug('✓ No changes detected', {
            lastCheck: since,
            currentTimestamp: changeData.timestamp
          });
        }
        
        // Update last check timestamp
        lastCheckTimestamp.current = changeData.timestamp;
        
      } catch (error) {
        logger.error('❌ CDC polling failed', { error: error.message });
      }
    }, 30000); // 30 seconds
  };

  // 🆕 PUBLIC API: Manual refresh
  const refreshUserInfo = async () => {
    logger.info('🔄 Manual user info refresh requested');
    try {
      await loadUserInfo();
      logger.info('✅ Manual refresh completed');
    } catch (error) {
      logger.error('❌ Manual refresh failed', { error: error.message });
      throw error;
    }
  };

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
        
        // 🔧 S login-required už nepotřebujeme manuální logout handling
        // Keycloak automaticky redirectne na login při odhlášení
        
        // Always initialize Keycloak with login-required (auto-login)
        const keycloakInstance = await keycloakService.initKeycloakOnce();
        
        if (keycloakInstance && keycloakInstance.authenticated) {
          logger.info('✅ User is authenticated');
          
          // Set token in API service first
          const token = keycloakService.getToken();
          await apiService.createSession(token);
          
          try {
            // 🆕 Load complete user info using centralized method
            await loadUserInfo();
            
            setIsAuthenticated(true);
            
            // 🆕 Start CDC polling for automatic updates
            startCdcPolling();
            
          } catch (error) {
            // Fallback to JWT data if API call fails
            logger.warn('Failed to load complete user data, using JWT fallback', { error: error.message });
            const userInfo = keycloakService.getUserInfo();
            if (userInfo) {
              setUser(userInfo);
              setIsAuthenticated(true);
            }
          }
          
          // Clear logout flags on successful authentication
          localStorage.removeItem('logout-completed');
          localStorage.removeItem('prevent-auto-login');
        } else {
          logger.info('ℹ️ User not authenticated - ready for manual login');
          setIsAuthenticated(false);
          // ❌ REMOVED: setShowLoggedOut(true) - nepotřebné
        }
        
        setKeycloakInitialized(true);
      } catch (error) {
        console.error('❌ [AUTH] Auth initialization failed:', error.message);
        setError(`Chyba při inicializaci: ${error.message}`);
      } finally {
        setLoading(false);
        isInitializingRef.current = false;
      }
    };

    initializeAuth();
    
    // 🆕 Cleanup CDC polling on unmount
    return () => {
      if (cdcIntervalRef.current) {
        clearInterval(cdcIntervalRef.current);
        logger.info('🛑 CDC polling stopped');
      }
    };
  }, []);

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
      if (!keycloakInitialized || !keycloakService.keycloak) {
        logger.info('⏳ Keycloak not ready, initializing first...');
        await keycloakService.initKeycloakOnce();
        setKeycloakInitialized(true);
      }
      
      keycloakService.login();
    } catch (error) {
      console.error('❌ [AUTH] Login failed:', error.message);
      hasTriedLoginRef.current = false;
    }
  };

  // Handle logout
  const handleLogout = async () => {
    try {
      logger.info('🚪 Logout initiated');
      
      // 🆕 Stop CDC polling
      if (cdcIntervalRef.current) {
        clearInterval(cdcIntervalRef.current);
        cdcIntervalRef.current = null;
        logger.info('🛑 CDC polling stopped');
      }
      
      // Set logout flags
      localStorage.setItem('logout-completed', Date.now().toString());
      localStorage.setItem('prevent-auto-login', 'true');
      
      // Clear API session
      await apiService.logout();
      
      // Clear state - ale NEPŘEKRESLUJEME, redirect proběhne okamžitě
      setUser(null);
      setIsAuthenticated(false);
      // ❌ REMOVED: setShowLoggedOut(true) - způsobovalo zobrazení LoginPage před redirectem
      
      // Reset ref guards
      hasTriedLoginRef.current = false;
      lastCheckTimestamp.current = null;
      
      // Logout from Keycloak - tato funkce OKAMŽITĚ redirectuje na Keycloak
      // takže žádný další kód se neprovede
      await keycloakService.logout();
      
    } catch (error) {
      console.error('❌ [AUTH] Logout failed:', error.message);
    }
  };

  const contextValue = {
    loading,
    error,
    user,
    isAuthenticated,
    keycloakInitialized,
    // ❌ REMOVED: showLoggedOut - nepotřebné
    login: handleLogin,
    logout: handleLogout,
    refreshUserInfo, // 🆕 Public API for manual refresh
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