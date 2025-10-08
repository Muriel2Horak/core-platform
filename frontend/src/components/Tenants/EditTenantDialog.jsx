import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Save as SaveIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

export const EditTenantDialog = ({ open, tenant, onClose, onSuccess }) => {
  const [displayName, setDisplayName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (tenant) {
      setDisplayName(tenant.displayName || '');
    }
  }, [tenant]);

  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);

      logger.userAction('TENANT_UPDATE_ATTEMPT', { key: tenant.key });

      await apiService.updateTenant(tenant.key, {
        displayName: displayName,
      });

      logger.userAction('TENANT_UPDATED', { key: tenant.key });

      onSuccess && onSuccess();
      handleClose();

    } catch (err) {
      logger.error('Failed to update tenant', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se aktualizovat tenant');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setDisplayName('');
    setError(null);
    onClose();
  };

  if (!tenant) return null;

  return (
    <Dialog 
      open={open} 
      onClose={handleClose} 
      maxWidth="sm" 
      fullWidth
      PaperProps={{
        sx: {
          background: 'rgba(255, 255, 255, 0.95) !important',
          backdropFilter: 'blur(10px)',
          borderRadius: 2,
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
        }
      }}
    >
      <DialogTitle>Upravit tenant: {tenant.key}</DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Alert severity="info" sx={{ mb: 2 }}>
          Tenant key nelze změnit. Pro změnu struktury vytvořte nový tenant.
        </Alert>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Tenant Key"
            value={tenant.key || ''}
            fullWidth
            disabled
            helperText="Key je immutable"
          />

          <TextField
            label="ID"
            value={tenant.id || 'N/A'}
            fullWidth
            disabled
            helperText="Unikátní identifikátor"
          />

          <TextField
            label="Název tenantu"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            fullWidth
            disabled={loading}
            autoFocus
            helperText="Zobrazovaný název tenantu"
          />

          <TextField
            label="Keycloak Realm"
            value={tenant.realm || tenant.key || 'N/A'}
            fullWidth
            disabled
            helperText="Jméno Keycloak realmu"
          />

          <TextField
            label="Subdoména"
            value={tenant.subdomain || `${tenant.key}.core-platform.local`}
            fullWidth
            disabled
            helperText="URL subdoména pro tento tenant"
          />
        </Box>
      </DialogContent>

      <DialogActions>
        <Button 
          onClick={handleClose} 
          disabled={loading}
          startIcon={<CloseIcon />}
        >
          Zrušit
        </Button>
        <Button 
          onClick={handleSubmit}
          variant="contained"
          disabled={loading}
          startIcon={loading ? <CircularProgress size={20} /> : <SaveIcon />}
        >
          Uložit změny
        </Button>
      </DialogActions>
    </Dialog>
  );
};

EditTenantDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  tenant: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
