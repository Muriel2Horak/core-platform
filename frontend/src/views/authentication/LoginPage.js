import React, { useState } from 'react';
import {
  Box,
  Paper,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress
} from '@mui/material';
import authService from '../../services/auth';

const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const data = await response.json();
        
        // Uložení tokenů do localStorage
        localStorage.setItem('keycloak-token', data.accessToken);
        localStorage.setItem('keycloak-refresh-token', data.refreshToken);
        
        // Načtení redirect URL z query parametrů
        const urlParams = new URLSearchParams(window.location.search);
        const redirectUrl = urlParams.get('redirect') || '/';
        
        // Přesměrování zpět do aplikace
        window.location.href = redirectUrl;
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Přihlášení se nezdařilo');
      }
    } catch (err) {
      setError('Chyba připojení k serveru');
    } finally {
      setIsLoading(false);
    }
  };

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
          maxWidth: 400,
          width: '100%'
        }}
      >
        <Typography variant="h4" component="h1" gutterBottom align="center">
          Core Platform
        </Typography>
        
        <Typography variant="h6" component="h2" gutterBottom align="center" color="textSecondary">
          Přihlášení
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="Uživatelské jméno"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            margin="normal"
            required
            disabled={isLoading}
            autoFocus
          />
          
          <TextField
            fullWidth
            type="password"
            label="Heslo"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            margin="normal"
            required
            disabled={isLoading}
          />

          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}
            disabled={isLoading}
            size="large"
          >
            {isLoading ? (
              <CircularProgress size={24} />
            ) : (
              'Přihlásit se'
            )}
          </Button>
        </Box>

        <Typography variant="body2" color="textSecondary" align="center" sx={{ mt: 2 }}>
          Testovací údaje: test / Test.1234
        </Typography>
      </Paper>
    </Box>
  );
};

export default LoginPage;