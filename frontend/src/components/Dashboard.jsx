import { useState, useEffect } from 'react';

import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Avatar,
  Chip,
  Alert,
} from '@mui/material';
import {
  Person as PersonIcon,
  Business as BusinessIcon,
  Security as SecurityIcon,
  Dashboard as DashboardIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';

function Dashboard({ user }) {
  const [stats, setStats] = useState({
    users: 0,
    tenants: 0,
    roles: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load dashboard statistics
  const loadStats = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const promises = [];
      
      // Load users if user has permission
      if (user?.roles?.includes('CORE_ROLE_USER_MANAGER') || user?.roles?.includes('CORE_ROLE_ADMIN')) {
        promises.push(apiService.getUsers().then(users => ({ type: 'users', data: users })));
      }
      
      // Load tenants if admin
      if (user?.roles?.includes('CORE_ROLE_ADMIN')) {
        promises.push(apiService.getTenants().then(tenants => ({ type: 'tenants', data: tenants })));
      }
      
      // Load roles
      promises.push(apiService.getRoles().then(roles => ({ type: 'roles', data: roles })));
      
      const results = await Promise.allSettled(promises);
      const newStats = { ...stats };
      
      results.forEach((result) => {
        if (result.status === 'fulfilled') {
          const { type, data } = result.value;
          if (Array.isArray(data)) {
            newStats[type] = data.length;
          }
        }
      });
      
      setStats(newStats);
      logger.info('Dashboard stats loaded', newStats);
    } catch (error) {
      logger.error('Failed to load dashboard stats', { error: error.message });
      setError('Nepodařilo se načíst statistiky.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Načteme statistiky pouze pokud máme user objekt
    if (user) {
      loadStats();
    }
  }, [user]); // ✅ Přidána závislost na user objekt

  // Get user display name
  const getUserDisplayName = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user?.username || user?.email || 'Uživatel';
  };

  // Get user roles display
  const getUserRoles = () => {
    if (!user?.roles || user.roles.length === 0) {
      return ['Běžný uživatel'];
    }
    
    return user.roles.map(role => {
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

  const statsCards = [
    {
      title: 'Uživatelé',
      value: stats.users,
      icon: <PersonIcon />,
      color: 'primary',
      show: user?.roles?.includes('CORE_ROLE_USER_MANAGER') || user?.roles?.includes('CORE_ROLE_ADMIN'),
    },
    {
      title: 'Tenanti',
      value: stats.tenants,
      icon: <BusinessIcon />,
      color: 'secondary',
      show: user?.roles?.includes('CORE_ROLE_ADMIN'),
    },
    {
      title: 'Role',
      value: stats.roles,
      icon: <SecurityIcon />,
      color: 'success',
      show: true,
    },
  ].filter(card => card.show);

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
        Dashboard
      </Typography>
      
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Vítejte v Core Platform. Zde najdete přehled vašeho systému.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Welcome Card */}
        <Grid item xs={12} md={8}>
          <Card sx={{
            background: theme => theme.palette.mode === 'dark'
              ? 'rgba(30, 30, 30, 0.6)'
              : 'rgba(255, 255, 255, 0.7)',
            backdropFilter: 'blur(20px)',
            border: theme => `1px solid ${theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)'}`,
          }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Avatar sx={{ 
                  bgcolor: 'primary.main', 
                  mr: 2, 
                  width: 56, 
                  height: 56,
                  boxShadow: theme => theme.palette.mode === 'dark'
                    ? '0 4px 20px rgba(25, 118, 210, 0.3)'
                    : '0 4px 20px rgba(25, 118, 210, 0.2)'
                }}>
                  <DashboardIcon />
                </Avatar>
                <Box>
                  <Typography variant="h5" component="h2" gutterBottom>
                    Vítejte, {getUserDisplayName()}!
                  </Typography>
                  <Typography variant="body1" color="text.secondary">
                    Tenant: <strong>{user?.tenant || 'Neuvedeno'}</strong>
                  </Typography>
                </Box>
              </Box>
              
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {getUserRoles().map((role, index) => (
                  <Chip
                    key={index}
                    label={role}
                    color="primary"
                    variant="outlined"
                    size="small"
                  />
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* User Info Card */}
        <Grid item xs={12} md={4}>
          <Card sx={{
            background: theme => theme.palette.mode === 'dark'
              ? 'rgba(30, 30, 30, 0.6)'
              : 'rgba(255, 255, 255, 0.7)',
            backdropFilter: 'blur(20px)',
            border: theme => `1px solid ${theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)'}`,
          }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" component="h3" gutterBottom>
                Informace o účtu
              </Typography>
              
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <Typography variant="body2">
                  <strong>Email:</strong> {user?.email || 'Neuvedeno'}
                </Typography>
                <Typography variant="body2">
                  <strong>Uživatelské jméno:</strong> {user?.username || 'Neuvedeno'}
                </Typography>
                <Typography variant="body2">
                  <strong>Tenant:</strong> {user?.tenant || 'Neuvedeno'}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Statistics Cards */}
        {statsCards.map((card, index) => (
          <Grid item xs={12} sm={6} md={4} key={index}>
            <Card sx={{
              background: theme => theme.palette.mode === 'dark'
                ? 'rgba(30, 30, 30, 0.6)'
                : 'rgba(255, 255, 255, 0.7)',
              backdropFilter: 'blur(20px)',
              border: theme => `1px solid ${theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)'}`,
              transition: 'all 0.3s ease',
              '&:hover': {
                transform: 'translateY(-4px)',
                boxShadow: theme => theme.palette.mode === 'dark'
                  ? '0 12px 40px rgba(0,0,0,0.4)'
                  : '0 12px 40px rgba(0,0,0,0.1)',
              }
            }}>
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Avatar sx={{ 
                    bgcolor: `${card.color}.main`, 
                    mr: 2,
                    boxShadow: theme => theme.palette.mode === 'dark'
                      ? `0 4px 20px rgba(25, 118, 210, 0.3)`
                      : `0 4px 20px rgba(25, 118, 210, 0.2)`
                  }}>
                    {card.icon}
                  </Avatar>
                  <Box>
                    <Typography variant="h4" component="div" sx={{ fontWeight: 600 }}>
                      {loading ? '...' : card.value}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {card.title}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

Dashboard.propTypes = {
  user: UserPropType,
};

export default Dashboard;