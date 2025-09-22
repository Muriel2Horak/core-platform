import './App.css'
import React, { useEffect, useState } from 'react';
import { CssBaseline, ThemeProvider, Box, CircularProgress, Typography } from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { baselightTheme } from "./theme/DefaultColors";
import { RouterProvider } from 'react-router';
import router from "./routes/Router.js"
import keycloakService from './services/keycloakService';

function App() {
  const theme = baselightTheme;
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [initializationError, setInitializationError] = useState(null);

  useEffect(() => {
    // 🔑 Inicializuj Keycloak při startu aplikace
    const initKeycloak = async () => {
      try {
        console.log('🚀 App: Inicializuji Keycloak...');
        const authenticated = await keycloakService.initialize();
        console.log('✅ App: Keycloak úspěšně inicializován, přihlášen:', authenticated);
        setKeycloakInitialized(true);
        setIsAuthenticated(authenticated);
      } catch (error) {
        console.error('❌ App: Chyba při inicializaci Keycloak:', error);
        setInitializationError(error.message);
      }
    };

    initKeycloak();
  }, []); // Prázdné pole závislostí - spustí se jen jednou při mount

  // Zobraz loading pouze během inicializace Keycloak
  if (!keycloakInitialized && !initializationError) {
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
          <Typography variant="h6">Inicializuji autentizaci...</Typography>
          <Typography variant="body2" color="text.secondary">
            Připojuji se ke Keycloak serveru
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
    return null;
  }

  // Keycloak inicializován a uživatel přihlášen - zobraz aplikaci
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <RouterProvider router={router} />
      </LocalizationProvider>
    </ThemeProvider>
  );
}

export default App