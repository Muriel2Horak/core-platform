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
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

export const TenantUsersDialog = ({ open, tenant, onClose }) => {
  const [usersData, setUsersData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open && tenant) {
      loadUsers();
    }
  }, [open, tenant]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiService.getTenantUsers(tenant.key);
      setUsersData(response);

      logger.info('Tenant users loaded', { 
        tenant: tenant.key, 
        count: response.userCount 
      });

    } catch (err) {
      logger.error('Failed to load tenant users', { error: err.message });
      setError('Nepodařilo se načíst uživatele');
    } finally {
      setLoading(false);
    }
  };

  const getUserInitials = (user) => {
    if (user.firstName && user.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    return (user.username || user.email || 'U').substring(0, 2).toUpperCase();
  };

  const handleClose = () => {
    setUsersData(null);
    setError(null);
    onClose();
  };

  if (!tenant) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PeopleIcon />
          <Typography variant="h6">Uživatelé: {tenant.displayName || tenant.key}</Typography>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : usersData ? (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Celkem uživatelů: <strong>{usersData.userCount || 0}</strong>
              </Typography>
            </Box>

            {usersData.userCount === 0 ? (
              <Box sx={{ textAlign: 'center', p: 3 }}>
                <PeopleIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                <Typography variant="h6" color="text.secondary">
                  Žádní uživatelé
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Tento tenant nemá žádné uživatele
                </Typography>
              </Box>
            ) : (
              <Alert severity="info">
                Tenant má {usersData.userCount} uživatel(ů). 
                Pro detailní seznam použijte sekci "Správa uživatelů" s filtrem tenantu.
              </Alert>
            )}
          </Box>
        ) : null}
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

TenantUsersDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  tenant: PropTypes.object,
  onClose: PropTypes.func.isRequired,
};
