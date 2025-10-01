import './App.css'
import React, { useEffect, useState } from 'react';
import { CssBaseline, ThemeProvider, Box, CircularProgress, Typography } from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { baselightTheme } from "./theme/DefaultColors";
import { RouterProvider } from 'react-router-dom';
import router from "./routes/Router.js"
import keycloakService from './services/keycloakService';
import { useTenant } from './context/TenantContext';
import ErrorBoundary from './components/ErrorBoundary.tsx';

function App() {
  const theme = baselightTheme;
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [initializationError, setInitializationError] = useState(null);
  const { tenant, setTenant, isTenantLoading, setIsTenantLoading } = useTenant();

  useEffect(() => {
    // 🔑 Inicializuj Keycloak a odvoď tenanta
    const initAuth = async () => {
      try {
        console.log('🚀 App: Inicializuji Keycloak...');
        const keycloak = await keycloakService.init();
        const authenticated = keycloak?.authenticated || false;
        
        console.log('✅ App: Keycloak úspěšně inicializován, přihlášen:', authenticated);
        setKeycloakInitialized(true);
        setIsAuthenticated(authenticated);

        if (authenticated) {
          const derivedTenant = keycloakService.getTenant();
          setTenant(derivedTenant);
        }
      } catch (error) {
        console.error('❌ App: Chyba při inicializaci Keycloak:', error);
        setInitializationError(error.message);
      } finally {
        setIsTenantLoading(false);
      }
    };

    initAuth();

    // Listener pro revalidaci tenanta po obnovení tokenu
    const handleTokenRefresh = (event) => {
      console.log('🔄 App: Token byl obnoven, revaliduji tenanta...');
      const newTenant = event.detail.tenant;
      if (newTenant && newTenant !== tenant) {
        setTenant(newTenant);
        console.log(`✅ App: Tenant aktualizován na '${newTenant}'`);
      }
    };

    window.addEventListener('keycloak-token-refreshed', handleTokenRefresh);

    return () => {
      window.removeEventListener('keycloak-token-refreshed', handleTokenRefresh);
    };
  }, [setTenant, setIsTenantLoading, tenant]);

  // Zobraz loading, dokud se neinicializuje Keycloak a nenačte tenant
  if (!keycloakInitialized || isTenantLoading) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
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
          <Typography variant="h6">Inicializuji aplikaci...</Typography>
          <Typography variant="body2" color="text.secondary">
            Připojuji se k autentizační službě a načítám konfiguraci...
          </Typography>
        </Box>
      </ThemeProvider>
    );
  }

  // Zobraz chybu pokud inicializace selhala
  if (initializationError) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            gap: 2,
            textAlign: 'center',
            p: 3
          }}
        >
          <Typography variant="h5" color="error">
            Chyba při přihlašování
          </Typography>
          <Typography variant="body1">
            {initializationError}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Zkuste obnovit stránku nebo kontaktujte správce systému.
          </Typography>
        </Box>
      </ThemeProvider>
    );
  }

  // Zobraz login stránku pokud uživatel není přihlášen
  if (!isAuthenticated) {
    // Automatický redirect na Keycloak login
    keycloakService.login();
    return null; // Během přesměrování nic nerenderujeme
  }

  // Guard pro neznámého tenanta
  if (!tenant) {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100vh',
                gap: 2,
                textAlign: 'center',
                p: 3
              }}
            >
              <Typography variant="h5" color="error">
                Neznámý Tenant
              </Typography>
              <Typography variant="body1">
                Nepodařilo se identifikovat tenanta. Zkuste se přihlásit znovu.
              </Typography>
            </Box>
        </ThemeProvider>
    );
  }

  // Keycloak inicializován, tenant načten a uživatel přihlášen - zobraz aplikaci
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <ErrorBoundary>
          <RouterProvider router={router} />
        </ErrorBoundary>
      </LocalizationProvider>
    </ThemeProvider>
  );
}

export default App