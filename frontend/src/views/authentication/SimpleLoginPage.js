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

const SimpleLoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  // Lazy loading pro logger
  const _log = (message, extra = {}) => {
    try {
      console.log(`🔐 ${message}`, extra);
      // Async load logger pro strukturované logy
      import('../../services/logger').then(module => {
        module.default.auth(message, extra);
      }).catch(() => {}); // Tichý fallback
    } catch (error) {
      console.log(`🔐 ${message}`, extra);
    }
  };

  const _logError = (message, extra = {}) => {
    try {
      console.error(`🔐 ${message}`, extra);
      import('../../services/logger').then(module => {
        module.default.error(message, extra);
      }).catch(() => {});
    } catch (error) {
      console.error(`🔐 ${message}`, extra);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    _log('LOGIN: Zahajuji přihlášení...', { username });

    try {
      _log('LOGIN: Odesílám API request na /api/auth/login');
      
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
        credentials: 'include', // Důležité pro cookies
      });

      _log('LOGIN: Odpověď ze serveru', {
        status: response.status,
        ok: response.ok,
        headers: Object.fromEntries(response.headers.entries())
      });

      if (response.ok) {
        const data = await response.json();
        _log('LOGIN: Data z úspěšné odpovědi', data);
        
        // 🎯 PŘIDÁNO: Frontend auth log pro úspěšné přihlášení
        import('../../services/logger').then(module => {
          module.default.authLogin(true, {
            username: username,
            method: 'password',
            timestamp: new Date().toISOString()
          });
        }).catch(() => {});
        
        // Backend ukládá tokeny do cookies a vrací accessToken + uživatelská data
        // Uložíme token do localStorage pro kompatibilitu s AuthService
        if (data.accessToken) {
          localStorage.setItem('keycloak-token', data.accessToken);
          _log('LOGIN: Token uložen do localStorage');
        }
        
        _log('LOGIN: Přesměrovávám na hlavní stránku...');
        // Přihlášení úspěšné - přesměruj na hlavní stránku
        window.location.href = '/';
        
      } else {
        _logError('LOGIN: Chyba při přihlášení', { status: response.status });
        
        // 🎯 PŘIDÁNO: Frontend auth log pro neúspěšné přihlášení
        import('../../services/logger').then(module => {
          module.default.authLogin(false, {
            username: username,
            method: 'password',
            error: 'authentication_failed',
            status: response.status,
            timestamp: new Date().toISOString()
          });
        }).catch(() => {});
        
        let errorMessage = 'Přihlášení se nezdařilo';
        
        try {
          const errorData = await response.json();
          _logError('LOGIN: Error data', errorData);
          errorMessage = errorData.error || errorData.detail || errorMessage;
        } catch (parseError) {
          _logError('LOGIN: Nelze parsovat error response', parseError);
          // Pokud se nepodaří parsovat error response, použijeme default zprávu
          errorMessage = `Chyba serveru (${response.status})`;
        }
        
        setError(errorMessage);
      }
    } catch (err) {
      _logError('LOGIN: Exception při přihlášení', err);
      
      // 🎯 PŘIDÁNO: Frontend auth log pro síťové chyby
      import('../../services/logger').then(module => {
        module.default.authLogin(false, {
          username: username,
          method: 'password',
          error: 'network_error',
          details: err.message,
          timestamp: new Date().toISOString()
        });
      }).catch(() => {});
      
      setError('Chyba připojení k serveru');
    } finally {
      setIsLoading(false);
      _log('LOGIN: Přihlášení dokončeno');
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
            disabled={isLoading || !username || !password}
            size="large"
          >
            {isLoading ? (
              <CircularProgress size={24} />
            ) : (
              'Přihlásit se'
            )}
          </Button>
        </Box>

        {/* 🚨 SECURITY FIX: Removed hardcoded test credentials for security */}
      </Paper>
    </Box>
  );
};

export default SimpleLoginPage;