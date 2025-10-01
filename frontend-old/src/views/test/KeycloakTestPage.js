import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  Chip,
  Grid,
  CircularProgress
} from '@mui/material';
import keycloakService from '../../services/keycloakService';

const KeycloakTestPage = () => {
  const [userInfo, setUserInfo] = useState(null);
  const [apiResponse, setApiResponse] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Načti informace o uživateli
    if (keycloakService.isAuthenticated()) {
      setUserInfo(keycloakService.getUserInfo());
    }
  }, []);

  const testApiCall = async (endpoint) => {
    setLoading(true);
    setError(null);
    setApiResponse(null);

    try {
      const response = await keycloakService.apiCall(`/api${endpoint}`);
      
      if (response.ok) {
        const data = await response.json();
        setApiResponse(data);
      } else {
        throw new Error(`API call failed: ${response.status} ${response.statusText}`);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    keycloakService.logout();
  };

  const openAccountConsole = () => {
    keycloakService.openAccountConsole();
  };

  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        🔑 Keycloak Integration Test
      </Typography>

      <Grid container spacing={3}>
        {/* User Info Card */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                👤 User Information
              </Typography>
              
              {userInfo ? (
                <Box>
                  <Typography><strong>Username:</strong> {userInfo.username}</Typography>
                  <Typography><strong>Email:</strong> {userInfo.email}</Typography>
                  <Typography><strong>Name:</strong> {userInfo.firstName} {userInfo.lastName}</Typography>
                  
                  <Box sx={{ mt: 2 }}>
                    <Typography variant="subtitle2" gutterBottom>Roles:</Typography>
                    {userInfo.roles.map((role) => (
                      <Chip key={role} label={role} size="small" sx={{ mr: 1, mb: 1 }} />
                    ))}
                  </Box>

                  <Box sx={{ mt: 2 }}>
                    <Button 
                      variant="outlined" 
                      onClick={openAccountConsole}
                      sx={{ mr: 1 }}
                    >
                      🔧 Account Console
                    </Button>
                    <Button 
                      variant="outlined" 
                      color="error"
                      onClick={handleLogout}
                    >
                      🚪 Logout
                    </Button>
                  </Box>
                </Box>
              ) : (
                <Typography>No user information available</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* API Test Card */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                🧪 API Test Endpoints
              </Typography>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 2 }}>
                <Button 
                  variant="contained" 
                  onClick={() => testApiCall('/hello')}
                  disabled={loading}
                >
                  Test /api/hello (ROLE_USER required)
                </Button>
                
                <Button 
                  variant="contained" 
                  onClick={() => testApiCall('/admin')}
                  disabled={loading}
                >
                  Test /api/admin (admin role required)
                </Button>
                
                <Button 
                  variant="contained" 
                  onClick={() => testApiCall('/me')}
                  disabled={loading}
                >
                  Test /api/me (user info)
                </Button>
              </Box>

              {loading && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <CircularProgress size={20} />
                  <Typography>Testing API...</Typography>
                </Box>
              )}

              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  <strong>API Error:</strong> {error}
                </Alert>
              )}

              {apiResponse && (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    ✅ API Response:
                  </Typography>
                  <Box 
                    component="pre" 
                    sx={{ 
                      backgroundColor: '#f5f5f5', 
                      p: 2, 
                      borderRadius: 1,
                      fontSize: '0.875rem',
                      overflow: 'auto',
                      maxHeight: 300
                    }}
                  >
                    {JSON.stringify(apiResponse, null, 2)}
                  </Box>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Instructions Card */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                📋 Test Instructions
              </Typography>
              
              <Typography paragraph>
                <strong>1. User Info:</strong> Zobrazuje informace z JWT tokenu (username, email, roles)
              </Typography>
              
              <Typography paragraph>
                <strong>2. API Endpoints:</strong>
              </Typography>
              <ul>
                <li><code>/api/hello</code> - Vyžaduje ROLE_USER roli</li>
                <li><code>/api/admin</code> - Vyžaduje admin roli</li>
                <li><code>/api/me</code> - Vrací informace o uživateli z JWT tokenu</li>
              </ul>
              
              <Typography paragraph>
                <strong>3. Account Console:</strong> Otevře Keycloak Account Console pro změnu hesla/údajů
              </Typography>
              
              <Typography paragraph>
                <strong>4. Logout:</strong> Odhlásí uživatele a přesměruje na Keycloak logout
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default KeycloakTestPage;