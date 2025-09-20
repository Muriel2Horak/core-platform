import React, { useEffect, useState } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import authService from '../../services/auth';

const AuthGuard = ({ children }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Lazy loading pro logger
  const _log = (message, extra = {}) => {
    try {
      console.log(`🛡️ ${message}`, extra);
      // Async load logger pro strukturované logy
      import('../../services/logger').then(module => {
        // 🔧 FIX: Používám info() místo neexistující guard() metody
        module.default.info('AUTHGUARD', message, extra);
      }).catch(() => {}); // Tichý fallback
    } catch (error) {
      console.log(`🛡️ ${message}`, extra, error);
    }
  };

  const _logError = (message, extra = {}) => {
    try {
      console.error(`🛡️ ${message}`, extra);
      import('../../services/logger').then(module => {
        // 🔧 FIX: Používám error() místo neexistující guard() metody  
        module.default.error('AUTHGUARD_ERROR', message, extra);
      }).catch(() => {});
    } catch (error) {
      console.error(`🛡️ ${message}`, extra, error);
    }
  };

  useEffect(() => {
    const checkAuth = async () => {
      _log('AUTHGUARD: Zahajuji kontrolu autentizace...', {
        pathname: window.location.pathname,
        search: window.location.search
      });

      // Pokud jsme na login stránce, neprovádíme kontrolu autentizace
      if (window.location.pathname === '/login' || window.location.pathname === '/login/') {
        _log('AUTHGUARD: Jsme na login stránce, povolujeme bez kontroly');
        setIsAuthenticated(true);
        setIsLoading(false);
        return;
      }

      try {
        _log('AUTHGUARD: Volám authService.isAuthenticated()...');
        // 🔧 FIX: Vrátili jsme se k původnímu auth service
        const isValid = await authService.isAuthenticated();
        
        _log('AUTHGUARD: Výsledek kontroly autentizace', { isValid });
        
        if (isValid) {
          _log('AUTHGUARD: Uživatel je přihlášen - povolujeme přístup');
          setIsAuthenticated(true);
        } else {
          _log('AUTHGUARD: Uživatel není přihlášen - přesměrovávám na login stránku');
          // 🔧 FIX: Používáme původní redirectToLogin() metodu
          authService.redirectToLogin();
        }
      } catch (error) {
        _logError('AUTHGUARD: Chyba při kontrole autentizace', error);
        // 🔧 FIX: Při chybě také používáme původní redirectToLogin()
        authService.redirectToLogin();
      } finally {
        _log('AUTHGUARD: Dokončuji kontrolu autentizace');
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  _log('AUTHGUARD: Render stav', { isLoading, isAuthenticated });

  if (isLoading) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          gap: 2
        }}
      >
        <CircularProgress />
        <Typography variant="body1">Kontrola přihlášení...</Typography>
      </Box>
    );
  }

  return isAuthenticated ? children : null;
};

export default AuthGuard;