import ReactDOM from 'react-dom/client';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { coreMaterialTheme } from './theme';
import Account from './Account';

// Simulace Keycloak props - v reálném prostředí by tyto hodnoty poskytl Keycloak
const kcContext = {
  realm: {
    name: 'admin'  // Updated from core-platform to admin
  },
  url: {
    accountUrl: '/auth/realms/admin/account',
    passwordUrl: '/auth/realms/admin/account/password'
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

// 🔧 ODEBRÁN React.StrictMode - způsoboval duplicitní renderování
root.render(
  <ThemeProvider theme={coreMaterialTheme}>
    <CssBaseline />
    <Account {...kcContext} />
  </ThemeProvider>
);