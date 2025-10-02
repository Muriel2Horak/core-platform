import ReactDOM from 'react-dom/client';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { coreMaterialTheme } from './theme';
import Login from './Login';

// Simulace Keycloak props - v reálném prostředí by tyto hodnoty poskytl Keycloak
const keycloakProps = {
  url: {
    loginAction: window.location.href,
    registrationUrl: '/auth/realms/core-platform/login-actions/registration',
    loginResetCredentialsUrl: '/auth/realms/core-platform/login-actions/reset-credentials'
  },
  realm: {
    displayName: 'Core Platform'
  },
  client: {
    clientId: 'web'
  },
  login: {
    username: '',
    rememberMe: false
  },
  registrationDisabled: false,
  resetPasswordAllowed: true,
  social: {
    providers: []
  }
};

const root = ReactDOM.createRoot(document.getElementById('root')!);

// 🔧 ODEBRÁN React.StrictMode - způsoboval duplicitní renderování
root.render(
  <ThemeProvider theme={coreMaterialTheme}>
    <CssBaseline />
    <Login {...keycloakProps} />
  </ThemeProvider>
);