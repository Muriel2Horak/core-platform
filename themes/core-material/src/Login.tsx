import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Link,
  Divider,
  Paper,
  Container
} from '@mui/material';
import { LockOutlined, PersonOutlined } from '@mui/icons-material';

interface LoginProps {
  // Keycloak předá tyto props
  url?: {
    loginAction?: string;
    registrationUrl?: string;
    loginResetCredentialsUrl?: string;
  };
  realm?: {
    displayName?: string;
    displayNameHtml?: string;
  };
  client?: {
    clientId?: string;
  };
  message?: {
    type?: 'success' | 'warning' | 'error' | 'info';
    summary?: string;
  };
  login?: {
    username?: string;
    rememberMe?: boolean;
  };
  registrationDisabled?: boolean;
  resetPasswordAllowed?: boolean;
  social?: {
    providers?: Array<{
      alias: string;
      providerId: string;
      displayName: string;
      iconClasses?: string;
    }>;
  };
}

const Login: React.FC<LoginProps> = ({
  url = {},
  realm = {},
  client = {},
  message,
  login = {},
  registrationDisabled = true,
  resetPasswordAllowed = true,
  social = {}
}) => {
  const [username, setUsername] = useState(login.username || '');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(login.rememberMe || false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    
    // Keycloak form submission
    if (url.loginAction) {
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = url.loginAction;
      
      const usernameInput = document.createElement('input');
      usernameInput.type = 'hidden';
      usernameInput.name = 'username';
      usernameInput.value = username;
      form.appendChild(usernameInput);
      
      const passwordInput = document.createElement('input');
      passwordInput.type = 'hidden';
      passwordInput.name = 'password';
      passwordInput.value = password;
      form.appendChild(passwordInput);
      
      if (rememberMe) {
        const rememberInput = document.createElement('input');
        rememberInput.type = 'hidden';
        rememberInput.name = 'rememberMe';
        rememberInput.value = 'on';
        form.appendChild(rememberInput);
      }
      
      document.body.appendChild(form);
      form.submit();
    }
  };

  const getMessageColor = (type?: string) => {
    switch (type) {
      case 'success': return 'success';
      case 'warning': return 'warning';
      case 'error': return 'error';
      case 'info': return 'info';
      default: return 'info';
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2
      }}
    >
      <Container maxWidth="sm">
        <Paper
          elevation={24}
          sx={{
            borderRadius: 3,
            overflow: 'hidden'
          }}
        >
          <Card>
            <CardContent sx={{ p: 4 }}>
              {/* Header */}
              <Box textAlign="center" mb={3}>
                <Box
                  sx={{
                    width: 64,
                    height: 64,
                    borderRadius: '50%',
                    backgroundColor: 'primary.main',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    mx: 'auto',
                    mb: 2
                  }}
                >
                  <LockOutlined sx={{ color: 'white', fontSize: 32 }} />
                </Box>
                
                <Typography variant="h4" component="h1" gutterBottom>
                  Přihlášení
                </Typography>
                
                {realm.displayName && (
                  <Typography variant="body2" color="text.secondary">
                    {realm.displayName}
                  </Typography>
                )}
              </Box>

              {/* Zprávy */}
              {message?.summary && (
                <Alert 
                  severity={getMessageColor(message.type)} 
                  sx={{ mb: 3 }}
                >
                  {message.summary}
                </Alert>
              )}

              {/* Login Form */}
              <Box component="form" onSubmit={handleSubmit}>
                <TextField
                  fullWidth
                  id="username"
                  name="username"
                  label="Uživatelské jméno nebo email"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  variant="outlined"
                  margin="normal"
                  required
                  autoComplete="username"
                  autoFocus
                  InputProps={{
                    startAdornment: (
                      <PersonOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                    ),
                  }}
                />

                <TextField
                  fullWidth
                  id="password"
                  name="password"
                  label="Heslo"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  variant="outlined"
                  margin="normal"
                  required
                  autoComplete="current-password"
                  InputProps={{
                    startAdornment: (
                      <LockOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                    ),
                  }}
                />

                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  size="large"
                  disabled={loading}
                  sx={{ 
                    mt: 3, 
                    mb: 2,
                    py: 1.5,
                    fontSize: '1.1rem'
                  }}
                >
                  {loading ? 'Přihlašuji...' : 'Přihlásit se'}
                </Button>

                {/* Odkazy */}
                <Box sx={{ textAlign: 'center', mt: 2 }}>
                  {resetPasswordAllowed && url.loginResetCredentialsUrl && (
                    <>
                      <Link 
                        href={url.loginResetCredentialsUrl}
                        variant="body2"
                        sx={{ textDecoration: 'none' }}
                      >
                        Zapomněli jste heslo?
                      </Link>
                    </>
                  )}

                  {!registrationDisabled && url.registrationUrl && (
                    <>
                      {resetPasswordAllowed && <Divider sx={{ my: 2 }} />}
                      <Typography variant="body2" color="text.secondary">
                        Nemáte účet?{' '}
                        <Link 
                          href={url.registrationUrl}
                          sx={{ textDecoration: 'none' }}
                        >
                          Zaregistrujte se
                        </Link>
                      </Typography>
                    </>
                  )}
                </Box>

                {/* Social Login */}
                {social.providers && social.providers.length > 0 && (
                  <>
                    <Divider sx={{ my: 3 }}>
                      <Typography variant="body2" color="text.secondary">
                        nebo
                      </Typography>
                    </Divider>
                    
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                      {social.providers.map((provider) => (
                        <Button
                          key={provider.alias}
                          variant="outlined"
                          fullWidth
                          href={`/auth/realms/${realm.displayName}/broker/${provider.alias}/login`}
                          sx={{ py: 1.5 }}
                        >
                          {provider.displayName}
                        </Button>
                      ))}
                    </Box>
                  </>
                )}
              </Box>
            </CardContent>
          </Card>
        </Paper>
      </Container>
    </Box>
  );
};

export default Login;