import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Avatar,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  People as PeopleIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';

function Users({ user }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Check if user has permission to view users
  const hasPermission = user?.roles?.includes('CORE_ROLE_USER_MANAGER') || user?.roles?.includes('CORE_ROLE_ADMIN');

  // Load users
  const loadUsers = async () => {
    if (!hasPermission) {
      setError('Nemáte oprávnění k zobrazení seznamu uživatelů.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const usersData = await apiService.getUsers();
      setUsers(usersData || []);
      logger.info('Users loaded', { count: usersData?.length || 0 });
    } catch (error) {
      logger.error('Failed to load users', { error: error.message });
      setError('Nepodařilo se načíst seznam uživatelů.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  // Get user display name
  const getUserDisplayName = (userData) => {
    if (userData?.firstName && userData?.lastName) {
      return `${userData.firstName} ${userData.lastName}`;
    }
    return userData?.username || userData?.email || 'Neznámý uživatel';
  };

  // Get user initials for avatar
  const getUserInitials = (userData) => {
    const name = getUserDisplayName(userData);
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  // Get user roles display
  const getUserRoles = (userData) => {
    if (!userData?.roles || userData.roles.length === 0) {
      return ['Běžný uživatel'];
    }
    
    return userData.roles.map(role => {
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

  if (!hasPermission) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Uživatelé
        </Typography>
        
        <Alert severity="error">
          Nemáte oprávnění k zobrazení seznamu uživatelů.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
        Správa uživatelů
      </Typography>
      
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Zde můžete spravovat uživatele v systému.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : users.length === 0 ? (
            <Box sx={{ textAlign: 'center', p: 3 }}>
              <PeopleIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                Nebyli nalezeni žádní uživatelé
              </Typography>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Uživatel</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Tenant</TableCell>
                    <TableCell>Role</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {users.map((userData, index) => (
                    <TableRow key={userData.id || index} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Avatar sx={{ bgcolor: 'primary.main', mr: 2, width: 40, height: 40 }}>
                            {getUserInitials(userData)}
                          </Avatar>
                          <Box>
                            <Typography variant="body1" sx={{ fontWeight: 500 }}>
                              {getUserDisplayName(userData)}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {userData?.username}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {userData?.email || 'Neuvedeno'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {userData?.tenant || 'Neuvedeno'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {getUserRoles(userData).map((role, roleIndex) => (
                            <Chip
                              key={roleIndex}
                              label={role}
                              size="small"
                              color="primary"
                              variant="outlined"
                            />
                          ))}
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Users;