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
  Paper,
  Container
} from '@mui/material';
import { EmailOutlined } from '@mui/icons-material';

interface ResetPasswordProps {
  // Keycloak předá tyto props
  url?: {
    loginAction?: string;
    loginUrl?: string;
  };
  realm?: {
    displayName?: string;
  };
  message?: {
    type?: 'success' | 'warning' | 'error' | 'info';
    summary?: string;
  };
  auth?: {
    attemptedUsername?: string;
  };
}

const ResetPassword: React.FC<ResetPasswordProps> = ({
  url = {},
  realm = {},
  message,
  auth = {}
}) => {
  const [username, setUsername] = useState(auth.attemptedUsername || '');
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
                    backgroundColor: 'warning.main',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    mx: 'auto',
                    mb: 2
                  }}
                >
                  <EmailOutlined sx={{ color: 'white', fontSize: 32 }} />
                </Box>
                
                <Typography variant="h4" component="h1" gutterBottom>
                  Obnovit heslo
                </Typography>
                
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Zadejte vaše uživatelské jméno nebo email a my vám pošleme odkaz pro obnovení hesla
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

              {/* Reset Password Form */}
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
                      <EmailOutlined sx={{ color: 'text.secondary', mr: 1 }} />
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
                  {loading ? 'Odesílám...' : 'Odeslat odkaz pro obnovení'}
                </Button>

                {/* Odkaz zpět na login */}
                <Box sx={{ textAlign: 'center', mt: 2 }}>
                  {url.loginUrl && (
                    <Link 
                      href={url.loginUrl}
                      variant="body2"
                      sx={{ textDecoration: 'none' }}
                    >
                      ← Zpět na přihlášení
                    </Link>
                  )}
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Paper>
      </Container>
    </Box>
  );
};

export default ResetPassword;