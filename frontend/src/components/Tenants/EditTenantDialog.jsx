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
  const [formData, setFormData] = useState({
    displayName: '',
    subdomain: '',
    active: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (tenant) {
      setFormData({
        displayName: tenant.displayName || '',
        subdomain: tenant.subdomain || '',
        active: tenant.active !== false,
      });
    }
  }, [tenant]);

  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);

      logger.userAction('TENANT_UPDATE_ATTEMPT', { key: tenant.key });

      await apiService.updateTenant(tenant.id, formData);

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
    setFormData({ displayName: '', subdomain: '', active: true });
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
            value={formData.displayName}
            onChange={(e) => setFormData(prev => ({ ...prev, displayName: e.target.value }))}
            fullWidth
            disabled={loading}
            autoFocus
            helperText="Zobrazovaný název tenantu"
          />

          <TextField
            label="Subdoména"
            value={formData.subdomain}
            onChange={(e) => setFormData(prev => ({ ...prev, subdomain: e.target.value }))}
            fullWidth
            disabled={loading}
            helperText="URL subdoména pro tento tenant (např. tenant1)"
          />
          
          <TextField
            label="Vytvořeno"
            value={tenant.createdAt ? new Date(tenant.createdAt).toLocaleString('cs-CZ') : 'N/A'}
            fullWidth
            disabled
          />
          
          {tenant.createdBy && (
            <TextField
              label="Vytvořil"
              value={tenant.createdBy}
              fullWidth
              disabled
            />
          )}
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
