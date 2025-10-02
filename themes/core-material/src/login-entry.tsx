import ReactDOM from 'react-dom/client';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { coreMaterialTheme } from './theme';
import Login from './Login';

// Simulace Keycloak props - v reálném prostředí by tyto hodnoty poskytl Keycloak
const kcContext = {
  realm: {
    name: 'admin',  // Updated from core-platform to admin
    displayName: 'Admin Realm'
  },
  url: {
    loginAction: window.location.href,
    registrationUrl: '/auth/realms/admin/login-actions/registration',
    loginResetCredentialsUrl: '/auth/realms/admin/login-actions/reset-credentials'
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
    <Login {...kcContext} />
  </ThemeProvider>
);