import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Alert,
  CircularProgress,
  Typography,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Chip,
} from '@mui/material';
import {
  Close as CloseIcon,
  People as PeopleIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

/**
 * 👥 Role Users View
 * Zobrazení všech uživatelů s konkrétní rolí
 */
export const RoleUsersView = ({ open, role, onClose }) => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open && role) {
      loadUsers();
    }
  }, [open, role]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);

      const usersData = await apiService.getRoleUsers(role.name);
      setUsers(usersData || []);

      logger.info('Role users loaded', { 
        role: role.name, 
        count: usersData?.length || 0 
      });

    } catch (err) {
      logger.error('Failed to load role users', { error: err.message });
      setError('Nepodařilo se načíst seznam uživatelů');
    } finally {
      setLoading(false);
    }
  };

  const getUserDisplayName = (user) => {
    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user.username || user.email || 'Neznámý uživatel';
  };

  const getUserInitials = (user) => {
    const name = getUserDisplayName(user);
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  const handleClose = () => {
    setUsers([]);
    setError(null);
    onClose();
  };

  if (!role) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PeopleIcon />
          <Typography variant="h6">Uživatelé s rolí: {role.name}</Typography>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {role.description && (
          <Alert severity="info" sx={{ mb: 2 }}>
            {role.description}
          </Alert>
        )}

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : users.length === 0 ? (
          <Box sx={{ textAlign: 'center', p: 3 }}>
            <PeopleIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Žádní uživatelé s touto rolí
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Role není přiřazena žádnému uživateli
            </Typography>
          </Box>
        ) : (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Celkem uživatelů: <strong>{users.length}</strong>
              </Typography>
            </Box>

            <List>
              {users.map((user, index) => (
                <ListItem 
                  key={user.id || index}
                  sx={{ 
                    bgcolor: 'background.paper',
                    borderRadius: 1,
                    mb: 1,
                    border: 1,
                    borderColor: 'divider',
                  }}
                >
                  <ListItemAvatar>
                    <Avatar sx={{ bgcolor: 'primary.main' }}>
                      {getUserInitials(user)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body1">
                          {getUserDisplayName(user)}
                        </Typography>
                        {user.enabled && (
                          <Chip label="Aktivní" size="small" color="success" />
                        )}
                      </Box>
                    }
                    secondary={
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          {user.username}
                        </Typography>
                        {user.email && (
                          <Typography variant="caption" color="text.secondary">
                            {user.email}
                          </Typography>
                        )}
                      </Box>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button 
          onClick={handleClose}
          startIcon={<CloseIcon />}
        >
          Zavřít
        </Button>
      </DialogActions>
    </Dialog>
  );
};

RoleUsersView.propTypes = {
  open: PropTypes.bool.isRequired,
  role: PropTypes.object,
  onClose: PropTypes.func.isRequired,
};
