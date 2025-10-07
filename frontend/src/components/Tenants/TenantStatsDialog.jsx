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
  Grid,
  Card,
  CardContent,
  Chip,
} from '@mui/material';
import {
  Close as CloseIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  People as PeopleIcon,
  Business as BusinessIcon,
  Storage as StorageIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

export const TenantStatsDialog = ({ open, tenant, onClose }) => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open && tenant) {
      loadStats();
    }
  }, [open, tenant]);

  const loadStats = async () => {
    try {
      setLoading(true);
      setError(null);

      const statsData = await apiService.getTenantStats(tenant.key);
      setStats(statsData);

      logger.info('Tenant stats loaded', { tenant: tenant.key });

    } catch (err) {
      logger.error('Failed to load tenant stats', { error: err.message });
      setError('Nepodařilo se načíst statistiky');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setStats(null);
    setError(null);
    onClose();
  };

  if (!tenant) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <BusinessIcon />
          <Typography variant="h6">Statistiky: {tenant.displayName || tenant.key}</Typography>
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
        ) : stats ? (
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Card variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <PeopleIcon color="primary" />
                    <Typography variant="h6">Uživatelé</Typography>
                  </Box>
                  <Typography variant="h3">{stats.userCount || 0}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Celkový počet uživatelů
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <StorageIcon color="primary" />
                    <Typography variant="h6">Realm Status</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Chip
                      label={stats.realmExists ? 'Realm existuje' : 'Realm neexistuje'}
                      color={stats.realmExists ? 'success' : 'error'}
                      icon={stats.realmExists ? <CheckCircleIcon /> : <CancelIcon />}
                    />
                    {stats.realmExists && (
                      <Chip
                        label={stats.realmEnabled ? 'Aktivní' : 'Neaktivní'}
                        color={stats.realmEnabled ? 'success' : 'default'}
                      />
                    )}
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="h6" gutterBottom>Informace</Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Tenant Key:</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {stats.tenantKey}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Display Name:</Typography>
                      <Typography variant="body2">{stats.displayName}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Created At:</Typography>
                      <Typography variant="body2">{stats.createdAt}</Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
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

TenantStatsDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  tenant: PropTypes.object,
  onClose: PropTypes.func.isRequired,
};
