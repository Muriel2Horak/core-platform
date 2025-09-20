import React from 'react';
import ReactDOM from 'react-dom/client';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { coreMaterialTheme } from './theme';
import Account from './Account';

// Simulace Keycloak props - v reálném prostředí by tyto hodnoty poskytl Keycloak
const keycloakProps = {
  url: {
    accountUrl: '/auth/realms/core-platform/account',
    passwordUrl: '/auth/realms/core-platform/account/password'
  },
  realm: {
    displayName: 'Core Platform'
  },
  account: {
    username: 'demo-user',
    email: 'demo@example.com',
    firstName: 'Demo',
    lastName: 'User',
    emailVerified: true
  },
  stateChecker: 'state-token-here'
};

const root = ReactDOM.createRoot(document.getElementById('root')!);

root.render(
  <React.StrictMode>
    <ThemeProvider theme={coreMaterialTheme}>
      <CssBaseline />
      <Account {...keycloakProps} />
    </ThemeProvider>
  </React.StrictMode>
);