import React, { useEffect, useState } from 'react';
import { Box, Typography, CircularProgress, Paper } from '@mui/material';

const AuthPage = () => {
  const [error, setError] = useState('');

  useEffect(() => {
    // Keycloak konfigurace podle dokumentace
    const KEYCLOAK_CFG = {
      url: 'http://localhost:8081',
      realm: 'core-platform',
      clientId: 'web'
    };

    // URL kam přesměrovat po úspěšném přihlášení
    const urlParams = new URLSearchParams(window.location.search);
    const APP_URL = urlParams.get('redirect') || '/';

    // Dynamické načtení Keycloak JS
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/keycloak-js@23/dist/keycloak.min.js';
    script.onload = () => {
      // Inicializace Keycloak po načtení scriptu
      const keycloak = new window.Keycloak(KEYCLOAK_CFG);

      keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        flow: 'standard'
      }).then(function(authenticated) {
        if (authenticated) {
          // Uložení tokenu do localStorage
          localStorage.setItem('keycloak-token', keycloak.token);
          localStorage.setItem('keycloak-refresh-token', keycloak.refreshToken);
          localStorage.setItem('keycloak-id-token', keycloak.idToken);
          
          // Přesměrování zpět do aplikace
          window.location.href = window.location.origin + APP_URL;
        } else {
          setError('Přihlášení se nezdařilo');
        }
      }).catch(function(error) {
        console.error('Keycloak initialization error:', error);
        setError('Chyba při inicializaci přihlášení: ' + error.message);
      });
    };

    script.onerror = () => {
      setError('Nepodařilo se načíst Keycloak knihovnu');
    };

    document.head.appendChild(script);

    // Cleanup
    return () => {
      if (document.head.contains(script)) {
        document.head.removeChild(script);
      }
    };
  }, []);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: 2
      }}
    >
      <Paper
        elevation={10}
        sx={{
          padding: 4,
          borderRadius: 2,
          textAlign: 'center',
          maxWidth: 400,
          width: '100%'
        }}
      >
        <Typography variant="h4" component="h1" gutterBottom>
          Core Platform
        </Typography>
        
        {error ? (
          <Box>
            <Typography variant="body1" color="error" sx={{ mb: 2 }}>
              {error}
            </Typography>
          </Box>
        ) : (
          <Box>
            <Typography variant="body1" sx={{ mb: 3 }}>
              Přihlašování...
            </Typography>
            <CircularProgress />
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default AuthPage;