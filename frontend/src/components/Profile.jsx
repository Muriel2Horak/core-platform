import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Avatar,
  Chip,
  Alert,
  Divider,
} from '@mui/material';
import {
  Person as PersonIcon,
  Email as EmailIcon,
  Business as BusinessIcon,
  Security as SecurityIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';

function Profile({ user }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load user profile
  const loadProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const profileData = await apiService.getMe();
      setProfile(profileData);
      logger.info('User profile loaded');
    } catch (error) {
      logger.error('Failed to load user profile', { error: error.message });
      setError('Nepodařilo se načíst profil uživatele.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  // Get user display name
  const getUserDisplayName = () => {
    const data = profile || user;
    if (data?.firstName && data?.lastName) {
      return `${data.firstName} ${data.lastName}`;
    }
    return data?.username || data?.email || 'Uživatel';
  };

  // Get user initials for avatar
  const getUserInitials = () => {
    const name = getUserDisplayName();
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  // Get user roles display
  const getUserRoles = () => {
    const data = profile || user;
    if (!data?.roles || data.roles.length === 0) {
      return ['Běžný uživatel'];
    }
    
    return data.roles.map(role => {
      switch (role) {
        case 'CORE_ROLE_ADMIN':
          return 'Administrátor';
        case 'CORE_ROLE_USER_MANAGER':
          return 'Správce uživatelů';
        default:
          return role.replace('CORE_ROLE_', '');
      }
    });
  };

  const data = profile || user;

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
        Můj profil
      </Typography>
      
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Zde můžete zobrazit informace o vašem uživatelském účtu.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Profile Card */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Avatar sx={{ bgcolor: 'primary.main', mr: 3, width: 80, height: 80, fontSize: '1.5rem' }}>
                  {getUserInitials()}
                </Avatar>
                <Box>
                  <Typography variant="h4" component="h2" gutterBottom>
                    {getUserDisplayName()}
                  </Typography>
                  <Typography variant="h6" color="text.secondary" gutterBottom>
                    {data?.email || 'Neuvedeno'}
                  </Typography>
                  <Typography variant="body1" color="text.secondary">
                    Tenant: <strong>{data?.tenant || 'Neuvedeno'}</strong>
                  </Typography>
                </Box>
              </Box>
              
              <Divider sx={{ my: 2 }} />
              
              <Typography variant="h6" gutterBottom>
                Role a oprávnění
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 3 }}>
                {getUserRoles().map((role, index) => (
                  <Chip
                    key={index}
                    label={role}
                    color="primary"
                    variant="outlined"
                  />
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Details Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" component="h3" gutterBottom>
                Detailní informace
              </Typography>
              
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <PersonIcon sx={{ mr: 2, color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Uživatelské jméno
                    </Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>
                      {data?.username || 'Neuvedeno'}
                    </Typography>
                  </Box>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <EmailIcon sx={{ mr: 2, color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Email
                    </Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>
                      {data?.email || 'Neuvedeno'}
                    </Typography>
                  </Box>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <BusinessIcon sx={{ mr: 2, color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Tenant
                    </Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>
                      {data?.tenant || 'Neuvedeno'}
                    </Typography>
                  </Box>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <SecurityIcon sx={{ mr: 2, color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Počet rolí
                    </Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>
                      {data?.roles?.length || 0}
                    </Typography>
                  </Box>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Additional Info Card */}
        {(data?.firstName || data?.lastName) && (
          <Grid item xs={12}>
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" component="h3" gutterBottom>
                  Osobní údaje
                </Typography>
                
                <Grid container spacing={2}>
                  {data?.firstName && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2" color="text.secondary">
                        Křestní jméno
                      </Typography>
                      <Typography variant="body1" sx={{ fontWeight: 500 }}>
                        {data.firstName}
                      </Typography>
                    </Grid>
                  )}
                  
                  {data?.lastName && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2" color="text.secondary">
                        Příjmení
                      </Typography>
                      <Typography variant="body1" sx={{ fontWeight: 500 }}>
                        {data.lastName}
                      </Typography>
                    </Grid>
                  )}
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>
    </Box>
  );
}

export default Profile;